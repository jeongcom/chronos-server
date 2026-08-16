package com.chronos.infrastructure.device;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DevicePresenceMonitor {
    private final NamedParameterJdbcTemplate jdbc;
    private final long offlineSeconds;

    public DevicePresenceMonitor(NamedParameterJdbcTemplate jdbc,
            @Value("${chronos.device.offline-seconds:60}") long offlineSeconds) {
        this.jdbc = jdbc; this.offlineSeconds = offlineSeconds;
    }

    @Scheduled(fixedDelayString = "${chronos.device.offline-scan-ms:15000}")
    public void markStaleOffline() {
        jdbc.update("""
            UPDATE chronos.device SET status='OFFLINE', offline_at=NOW()
            WHERE status='ONLINE' AND (last_seen_at IS NULL OR last_seen_at < NOW() - (:seconds * INTERVAL '1 second'))
            """, new MapSqlParameterSource("seconds", offlineSeconds));
    }
}
