package com.chronos.gateway.protocol;

import java.time.Instant;

public record DeviceFrame(
        int protocolVersion,
        DeviceMessageType messageType,
        long sequence,
        Instant occurredAt,
        String deviceId,
        String spaceId,
        String payloadJson) {
}
