package com.chronos.infrastructure.postgres;

import com.chronos.application.port.OutboxPort;
import com.chronos.domain.event.StoredEvent;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresOutbox implements OutboxPort {
    private final JdbcClient jdbc; private final JsonMapper json;
    public PostgresOutbox(JdbcClient jdbc, JsonMapper json) { this.jdbc = jdbc; this.json = json; }
    @Override public void enqueue(String topic, String eventKey, StoredEvent event) {
        try {
            jdbc.sql("""
                INSERT INTO chronos.event_outbox(event_id,topic,event_key,payload)
                VALUES (:eventId,:topic,:eventKey,CAST(:payload AS jsonb))
                """).param("eventId", event.event().eventId()).param("topic", topic).param("eventKey", eventKey)
                .param("payload", json.writeValueAsString(event)).update();
        } catch (Exception e) { throw new IllegalStateException("Failed to enqueue outbox", e); }
    }
}
