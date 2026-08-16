package com.chronos.infrastructure.postgres;

import com.chronos.application.port.EventStore;
import com.chronos.domain.event.*;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.Instant;
import java.util.*;

@Repository
public class PostgresEventStore implements EventStore {
    private final JdbcClient jdbc;
    private final JsonMapper json;
    public PostgresEventStore(JdbcClient jdbc, JsonMapper json) { this.jdbc = jdbc; this.json = json; }

    @Override
    public StoredEvent append(ChronosEvent e) {
        String payload = write(e.payload());
        String headers = "{}";
        Long seq = jdbc.sql("""
            INSERT INTO chronos.event_store
            (event_id,event_type,schema_version,source_type,source_id,source_sequence,space_id,
             occurred_at,received_at,correlation_id,causation_id,confidence,payload,headers)
            VALUES (:eventId,:eventType,:schemaVersion,:sourceType,:sourceId,:sourceSequence,:spaceId,
                    :occurredAt,:receivedAt,:correlationId,:causationId,:confidence,CAST(:payload AS jsonb),CAST(:headers AS jsonb))
            RETURNING event_seq
            """)
            .param("eventId", e.eventId()).param("eventType", e.eventType()).param("schemaVersion", e.schemaVersion())
            .param("sourceType", e.sourceType()).param("sourceId", e.sourceId()).param("sourceSequence", e.sourceSequence())
            .param("spaceId", e.spaceId()).param("occurredAt", e.occurredAt()).param("receivedAt", e.receivedAt())
            .param("correlationId", e.correlationId()).param("causationId", e.causationId()).param("confidence", e.confidence())
            .param("payload", payload).param("headers", headers)
            .query(Long.class).single();
        return new StoredEvent(seq, e);
    }

    @Override
    public Optional<StoredEvent> findById(UUID eventId) {
        return jdbc.sql("SELECT * FROM chronos.event_store WHERE event_id=:id").param("id", eventId)
                .query(this::map).optional();
    }

    @Override
    public Optional<StoredEvent> findBySourceSequence(String sourceId, long sourceSequence) {
        return jdbc.sql("SELECT * FROM chronos.event_store WHERE source_id=:sourceId AND source_sequence=:sourceSequence")
                .param("sourceId", sourceId)
                .param("sourceSequence", sourceSequence)
                .query(this::map).optional();
    }

    @Override
    public List<StoredEvent> find(String spaceId, long afterEventSeq, Instant untilInclusive) {
        return jdbc.sql("""
            SELECT * FROM chronos.event_store
             WHERE space_id=:spaceId AND event_seq>:afterSeq AND occurred_at<=:until
             ORDER BY occurred_at ASC, event_seq ASC
            """).param("spaceId", spaceId).param("afterSeq", afterEventSeq).param("until", untilInclusive)
                .query(this::map).list();
    }

    private StoredEvent map(ResultSet rs, int row) throws java.sql.SQLException {
        try {
            Map<String,Object> payload = json.readValue(rs.getString("payload"), new TypeReference<>(){});
            ChronosEvent e = new ChronosEvent(
                    rs.getObject("event_id", UUID.class), rs.getString("event_type"), rs.getInt("schema_version"),
                    rs.getString("source_type"), rs.getString("source_id"), (Long) rs.getObject("source_sequence"), rs.getString("space_id"),
                    rs.getObject("occurred_at", java.time.OffsetDateTime.class).toInstant(), rs.getObject("received_at", java.time.OffsetDateTime.class).toInstant(),
                    rs.getObject("correlation_id", UUID.class), rs.getObject("causation_id", UUID.class), rs.getDouble("confidence"), payload);
            return new StoredEvent(rs.getLong("event_seq"), e);
        } catch (Exception ex) { throw new IllegalStateException("Failed to map event", ex); }
    }

    private String write(Object value) { try { return json.writeValueAsString(value); } catch (Exception e) { throw new IllegalArgumentException(e); } }
}
