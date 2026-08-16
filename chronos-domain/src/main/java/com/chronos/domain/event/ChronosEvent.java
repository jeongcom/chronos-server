package com.chronos.domain.event;

import java.time.Instant;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.UUID;

public record ChronosEvent(
        UUID eventId,
        String eventType,
        int schemaVersion,
        String sourceType,
        String sourceId,
        Long sourceSequence,
        String spaceId,
        Instant occurredAt,
        Instant receivedAt,
        UUID correlationId,
        UUID causationId,
        double confidence,
        Map<String, Object> payload) {

    public ChronosEvent {
        if (eventId == null) throw new IllegalArgumentException("eventId is required");
        if (eventType == null || eventType.isBlank()) throw new IllegalArgumentException("eventType is required");
        if (schemaVersion < 1) throw new IllegalArgumentException("schemaVersion must be >= 1");
        if (sourceType == null || sourceType.isBlank()) throw new IllegalArgumentException("sourceType is required");
        if (sourceId == null || sourceId.isBlank()) throw new IllegalArgumentException("sourceId is required");
        if (spaceId == null || spaceId.isBlank()) throw new IllegalArgumentException("spaceId is required");
        if (occurredAt == null) throw new IllegalArgumentException("occurredAt is required");
        if (receivedAt == null) throw new IllegalArgumentException("receivedAt is required");
        if (confidence < 0.0 || confidence > 1.0) throw new IllegalArgumentException("confidence must be 0..1");
        payload = payload == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(payload));
    }
}
