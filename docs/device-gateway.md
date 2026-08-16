# CHRONOS Device Gateway v0.1

## Purpose

The gateway terminates long-lived TCP device connections, reconstructs framed packets, converts vendor/device messages into the canonical CHRONOS gRPC event contract, batches events, and forwards them to `ChronosIngestService`.

The gateway never writes PostgreSQL/Kafka/Redis directly. The CHRONOS Server owns durability and canonical event ingestion.

## End-to-end path

```text
Device TCP :9100
  -> Netty frame decoder
  -> device session manager
  -> ChronosEventFactory
  -> BatchIngestDispatcher (100 events / 100 ms)
  -> gRPC PublishEvents
  -> CHRONOS Server
  -> PostgreSQL event_store + event_outbox (same transaction)
  -> Kafka chronos.events.v1
  -> WorldProjector
  -> Redis current world
```

## Protocol v1

All integers are big-endian.

| Offset | Size | Field |
|---:|---:|---|
| 0 | 2 | Magic `0x4348` (`CH`) |
| 2 | 1 | Protocol version (`1`) |
| 3 | 1 | Message type |
| 4 | 4 | Total frame length |
| 8 | 8 | Device source sequence |
| 16 | 8 | Occurred-at Unix epoch milliseconds |
| 24 | 2 | Device ID UTF-8 byte length |
| 26 | 2 | Space ID UTF-8 byte length |
| 28 | N | Device ID |
| ... | N | Space ID |
| ... | N | UTF-8 JSON payload |

Message types:

- `1`: `DEVICE.TEMPERATURE.CHANGED`
- `2`: `DOOR.OPENED`
- `3`: `DOOR.CLOSED`
- `4`: `LIGHT.TURNED_ON`
- `5`: `LIGHT.TURNED_OFF`
- `6`: `DEVICE.HEARTBEAT`
- `100`: `DEVICE.GENERIC.JSON`
- `127`: ACK (gateway -> device)

## ACK v1

```text
magic(2)
version(1)
type=127(1)
totalLength=28(4)
sourceSequence(8)
status(4)
eventSeq(8)
```

Status:

- `0`: accepted
- `1`: duplicate (already stored)
- `2`: invalid
- `3`: rejected/transient failure

A device should retain unacknowledged packets and resend them with the same `sourceSequence`. The server unique index on `(source_id, source_sequence)` makes retries idempotent and returns the previously stored `eventSeq` when possible.

## TCP robustness

`DeviceFrameDecoder` handles:

- TCP fragmentation: a packet may arrive over multiple reads.
- TCP coalescing: multiple frames may arrive in one read.
- Garbage bytes: decoder scans forward to the next `CH` magic.
- Frame size limits: defaults to 1 MiB.
- Protocol version validation.
- Device/space length validation.
- Idle connections: default read idle timeout is 90 seconds.

## Batch behavior

Default settings:

- Maximum 100 events per gRPC request.
- Flush every 100 ms if batch is not full.
- Bounded queue of 10,000 events.
- 5 second gRPC deadline.
- 3 transmission attempts with short exponential delay.

If the queue is full, the device receives a rejected ACK and is expected to retry later using the same sequence number.
