package com.chronos.gateway.session;

import io.netty.channel.Channel;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DeviceSessionManager {
    private final ConcurrentHashMap<String, DeviceSession> byDevice = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> deviceByChannel = new ConcurrentHashMap<>();

    public void register(String deviceId, Channel channel) {
        DeviceSession session = new DeviceSession(deviceId, channel, channel.remoteAddress(), Instant.now(), Instant.now());
        DeviceSession previous = byDevice.put(deviceId, session);
        deviceByChannel.put(channel.id().asLongText(), deviceId);
        if (previous != null && previous.channel() != channel && previous.channel().isActive()) {
            previous.channel().close();
        }
    }

    public void touch(String deviceId, Channel channel) {
        byDevice.compute(deviceId, (id, current) -> {
            if (current == null || current.channel() != channel) {
                return new DeviceSession(deviceId, channel, channel.remoteAddress(), Instant.now(), Instant.now());
            }
            return current.touch();
        });
        deviceByChannel.put(channel.id().asLongText(), deviceId);
    }

    public Optional<DeviceSession> find(String deviceId) {
        return Optional.ofNullable(byDevice.get(deviceId));
    }

    public void unregister(Channel channel) {
        String deviceId = deviceByChannel.remove(channel.id().asLongText());
        if (deviceId != null) {
            byDevice.computeIfPresent(deviceId, (id, session) -> session.channel() == channel ? null : session);
        }
    }

    public int activeCount() {
        return byDevice.size();
    }
}
