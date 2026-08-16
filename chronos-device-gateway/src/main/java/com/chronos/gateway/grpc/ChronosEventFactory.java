package com.chronos.gateway.grpc;

import com.chronos.contract.v1.ChronosEvent;
import com.chronos.gateway.protocol.DeviceFrame;
import com.chronos.gateway.protocol.DeviceMessageType;
import com.google.protobuf.Timestamp;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class ChronosEventFactory {
    public ChronosEvent create(DeviceFrame frame) {
        DeviceMessageType type = frame.messageType();
        if (type == DeviceMessageType.ACK) {
            throw new IllegalArgumentException("ACK is not an ingest event");
        }

        String eventType = type.eventType() != null ? type.eventType() : "DEVICE.GENERIC.JSON";
        String payload = frame.payloadJson() == null || frame.payloadJson().isBlank() ? "{}" : frame.payloadJson();

        return ChronosEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setEventType(eventType)
                .setSchemaVersion(1)
                .setSourceType("DEVICE")
                .setSourceId(frame.deviceId())
                .setSourceSequence(frame.sequence())
                .setSpaceId(frame.spaceId())
                .setOccurredAt(toTimestamp(frame.occurredAt()))
                .setReceivedAt(toTimestamp(Instant.now()))
                .setConfidence(1.0)
                .setPayloadJson(payload)
                .build();
    }

    private static Timestamp toTimestamp(Instant value) {
        return Timestamp.newBuilder()
                .setSeconds(value.getEpochSecond())
                .setNanos(value.getNano())
                .build();
    }
}
