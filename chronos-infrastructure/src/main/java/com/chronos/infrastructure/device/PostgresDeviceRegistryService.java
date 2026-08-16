package com.chronos.infrastructure.device;

import com.chronos.application.device.DeviceRegistryUseCase;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class PostgresDeviceRegistryService implements DeviceRegistryUseCase {
    private final NamedParameterJdbcTemplate jdbc;
    private final DeviceCredentialHasher hasher;
    private final RedisDeviceLeaseStore leases;

    public PostgresDeviceRegistryService(NamedParameterJdbcTemplate jdbc, DeviceCredentialHasher hasher,
                                         RedisDeviceLeaseStore leases) {
        this.jdbc = jdbc; this.hasher = hasher; this.leases = leases;
    }

    @Override @Transactional
    public RegisteredDevice register(RegisterDevice c) {
        String secret = hasher.newSecret();
        String salt = hasher.newSalt();
        String hash = hasher.hash(secret, salt);
        var p = new MapSqlParameterSource()
                .addValue("id", c.deviceId()).addValue("space", c.spaceId()).addValue("type", c.deviceType())
                .addValue("manufacturer", c.manufacturer()).addValue("model", c.model())
                .addValue("protocol", c.protocol()).addValue("protocolVersion", c.protocolVersion())
                .addValue("salt", salt).addValue("hash", hash);
        int n = jdbc.update("""
            INSERT INTO chronos.device(device_id,space_id,device_type,manufacturer,model,protocol,protocol_version,
                credential_salt,credential_hash,enabled,status)
            VALUES(:id,:space,:type,:manufacturer,:model,:protocol,:protocolVersion,:salt,:hash,TRUE,'OFFLINE')
            ON CONFLICT(device_id) DO UPDATE SET
                space_id=EXCLUDED.space_id, device_type=EXCLUDED.device_type, manufacturer=EXCLUDED.manufacturer,
                model=EXCLUDED.model, protocol=EXCLUDED.protocol, protocol_version=EXCLUDED.protocol_version,
                credential_salt=EXCLUDED.credential_salt, credential_hash=EXCLUDED.credential_hash, enabled=TRUE,
                status='OFFLINE', last_gateway_id=NULL, last_connection_id=NULL
            """, p);
        if (n != 1) throw new IllegalStateException("Device registration failed");
        Instant registeredAt = jdbc.query("SELECT registered_at FROM chronos.device WHERE device_id=:id",
                new MapSqlParameterSource("id", c.deviceId()), (rs,row)->rs.getTimestamp(1).toInstant()).getFirst();
        return new RegisteredDevice(c.deviceId(), secret, registeredAt);
    }

    @Override @Transactional
    public AuthResult authenticate(String deviceId, String secret, String gatewayId, String connectionId) {
        var rows = jdbc.query("""
            SELECT device_id,space_id,enabled,credential_salt,credential_hash,last_sequence
            FROM chronos.device WHERE device_id=:id
            """, new MapSqlParameterSource("id", deviceId), (rs, row) -> new AuthRow(
                rs.getString("device_id"), rs.getString("space_id"), rs.getBoolean("enabled"),
                rs.getString("credential_salt"), rs.getString("credential_hash"), (Long)rs.getObject("last_sequence")));
        if (rows.isEmpty()) return new AuthResult(false, "DEVICE_NOT_REGISTERED", "", 0);
        AuthRow row = rows.getFirst();
        if (!row.enabled) return new AuthResult(false, "DEVICE_DISABLED", row.spaceId, 0);
        if (!hasher.matches(secret, row.salt, row.hash)) return new AuthResult(false, "INVALID_CREDENTIAL", row.spaceId, 0);
        if (!leases.acquire(deviceId, gatewayId, connectionId)) return new AuthResult(false, "DEVICE_LEASE_HELD", row.spaceId, 0);
        jdbc.update("""
            UPDATE chronos.device SET status='ONLINE', authenticated_at=NOW(), last_seen_at=NOW(),
                last_gateway_id=:gateway, last_connection_id=:connection, offline_at=NULL
            WHERE device_id=:id
            """, new MapSqlParameterSource().addValue("id", deviceId).addValue("gateway", gatewayId).addValue("connection", connectionId));
        long expected = row.lastSequence == null ? 1 : row.lastSequence + 1;
        return new AuthResult(true, "AUTHENTICATED", row.spaceId, expected);
    }

    @Override @Transactional
    public boolean heartbeat(String deviceId, String gatewayId, String connectionId, long lastSequence) {
        if (!leases.renew(deviceId, gatewayId, connectionId)) return false;
        jdbc.update("""
            UPDATE chronos.device SET status='ONLINE', last_seen_at=NOW(), last_gateway_id=:gateway,
                last_connection_id=:connection,
                last_sequence=CASE WHEN last_sequence IS NULL THEN :seq ELSE GREATEST(last_sequence,:seq) END,
                offline_at=NULL WHERE device_id=:id
            """, new MapSqlParameterSource().addValue("id", deviceId).addValue("gateway", gatewayId)
                .addValue("connection", connectionId).addValue("seq", lastSequence));
        return true;
    }

    @Override @Transactional
    public void offline(String deviceId, String gatewayId, String connectionId, String reason) {
        leases.release(deviceId, gatewayId, connectionId);
        jdbc.update("""
            UPDATE chronos.device SET status='OFFLINE', offline_at=NOW()
            WHERE device_id=:id AND last_gateway_id=:gateway AND last_connection_id=:connection
            """, new MapSqlParameterSource().addValue("id", deviceId).addValue("gateway", gatewayId)
                .addValue("connection", connectionId));
    }

    @Override
    public Optional<DeviceView> find(String deviceId) {
        return jdbc.query("SELECT * FROM chronos.device WHERE device_id=:id", new MapSqlParameterSource("id", deviceId),
                (rs,row)->view(rs)).stream().findFirst();
    }

    @Override
    public List<DeviceView> list() {
        return jdbc.query("SELECT * FROM chronos.device ORDER BY device_id", (rs,row)->view(rs));
    }

    private static DeviceView view(ResultSet rs) throws SQLException {
        return new DeviceView(rs.getString("device_id"), rs.getString("space_id"), rs.getString("device_type"),
            rs.getBoolean("enabled"), rs.getString("status"), rs.getString("last_gateway_id"),
            (Long)rs.getObject("last_sequence"), rs.getTimestamp("registered_at").toInstant(),
            rs.getTimestamp("last_seen_at") == null ? null : rs.getTimestamp("last_seen_at").toInstant());
    }

    private record AuthRow(String deviceId,String spaceId,boolean enabled,String salt,String hash,Long lastSequence) {}
}
