package com.chronos.application.device;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DeviceRegistryUseCase {
    RegisteredDevice register(RegisterDevice command);
    AuthResult authenticate(String deviceId, String secret, String gatewayId, String connectionId);
    boolean heartbeat(String deviceId, String gatewayId, String connectionId, long lastSequence);
    void offline(String deviceId, String gatewayId, String connectionId, String reason);
    Optional<DeviceView> find(String deviceId);
    List<DeviceView> list();

    record RegisterDevice(String deviceId, String spaceId, String deviceType, String manufacturer,
                          String model, String protocol, String protocolVersion) {}
    record RegisteredDevice(String deviceId, String secret, Instant registeredAt) {}
    record AuthResult(boolean authenticated, String message, String spaceId, long expectedSequence) {}
    record DeviceView(String deviceId, String spaceId, String deviceType, boolean enabled, String status,
                      String lastGatewayId, Long lastSequence, Instant registeredAt, Instant lastSeenAt) {}
}
