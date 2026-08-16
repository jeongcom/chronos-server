package com.chronos.infrastructure.postgres;

import com.chronos.application.port.SnapshotStore;
import com.chronos.domain.snapshot.WorldSnapshot;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.*;

@Repository
public class PostgresSnapshotStore implements SnapshotStore {
    private final JdbcClient jdbc; private final JsonMapper json;
    public PostgresSnapshotStore(JdbcClient jdbc, JsonMapper json) { this.jdbc=jdbc; this.json=json; }
    @Override public Optional<WorldSnapshot> findLatest(String spaceId, Instant at) {
        return jdbc.sql("""
            SELECT snapshot_id,space_id,snapshot_at,last_event_seq,snapshot_data
              FROM chronos.world_snapshot
             WHERE space_id=:spaceId AND snapshot_at<=:at
             ORDER BY snapshot_at DESC LIMIT 1
            """).param("spaceId", spaceId).param("at", at).query((rs,row)-> {
                try {
                    Map<String,Object> state=json.readValue(rs.getString("snapshot_data"),new TypeReference<>(){});
                    return new WorldSnapshot(rs.getObject("snapshot_id",UUID.class),rs.getString("space_id"),
                        rs.getObject("snapshot_at",java.time.OffsetDateTime.class).toInstant(),rs.getLong("last_event_seq"),state);
                } catch(Exception e){ throw new IllegalStateException(e); }
            }).optional();
    }
}
