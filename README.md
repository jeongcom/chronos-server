# CHRONOS v0.2

Reference implementation of the CHRONOS temporal event platform and TCP device gateway.

## Modules

```text
chronos-contract          Protobuf/gRPC contract
chronos-domain            Pure Java temporal/event/world domain
chronos-application       Inbound use cases + outbound ports
chronos-infrastructure    PostgreSQL, Outbox, Kafka, Redis, Replay services
chronos-api               REST API
chronos-grpc              CHRONOS Server gRPC ingest service
chronos-boot              CHRONOS Server executable
chronos-device-gateway    Java + Netty TCP gateway executable
```

## End-to-end architecture

```text
Device
  | TCP :9100
  v
CHRONOS Device Gateway (Java + Netty)
  | gRPC PublishEvents :9090
  v
CHRONOS Server (Java 25 + Spring Boot 4.1)
  |
  +--> PostgreSQL event_store + event_outbox
  |        |
  |        v
  |      Kafka chronos.events.v1
  |        |
  |        v
  |      WorldProjector
  |        |
  |        v
  +<----- Redis current world
  |
  +--> Historical replay: Snapshot + EventStore
```

PostgreSQL is the source of truth. Kafka transports committed events. Redis is a disposable current-state projection.

## Ports

- Device TCP Gateway: `9100`
- HTTP REST: `8080`
- CHRONOS gRPC: `9090`
- PostgreSQL: `5432`
- Redis: `6379`
- Kafka host listener: `29092`

## Start everything with Docker

```bash
docker compose --profile apps up --build
```

To start only infrastructure:

```bash
docker compose up -d
```

With local JDK 25 + Gradle:

```bash
gradle :chronos-boot:bootRun
gradle :chronos-device-gateway:bootRun
```

## Simulate a device

```bash
python tools/device-simulator.py --device TEMP-001 --space LAB-001 --type temperature --temperature 24.3 --sequence 1
```

Test TCP fragmentation/reassembly:

```bash
python tools/device-simulator.py --split --count 10
```

## Query current world

```bash
curl http://localhost:8080/api/v1/world/LAB-001/current
```

## Historical replay

```bash
curl 'http://localhost:8080/api/v1/world/LAB-001/state?at=2026-08-17T02:30:00Z'
```

## Important semantics

1. `event_store` is append-only.
2. `event_store` + `event_outbox` are written in one DB transaction.
3. Kafka publication happens after DB commit.
4. Redis can be rebuilt from committed events.
5. Device protocol is isolated from the canonical CHRONOS gRPC contract.
6. All canonical timestamps are UTC.
7. Historical state is reconstructed by replay, never by mutating history.
