package com.chronos.gateway.session;

import io.netty.channel.Channel;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DeviceSessionManager {
    public enum SequenceDecision { EXPECTED, DUPLICATE, GAP }
    private final ConcurrentHashMap<String, DeviceSession> byDevice = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> deviceByChannel = new ConcurrentHashMap<>();

    public DeviceSession registerAuthenticated(String deviceId, String spaceId, long expectedSequence, Channel channel) {
        String connectionId = channel.id().asLongText();
        DeviceSession session = new DeviceSession(deviceId, spaceId, connectionId, channel, channel.remoteAddress(),
                Instant.now(), Instant.now(), expectedSequence, Math.max(0, expectedSequence - 1));
        DeviceSession previous = byDevice.put(deviceId, session);
        deviceByChannel.put(connectionId, deviceId);
        if (previous != null && previous.channel() != channel && previous.channel().isActive()) previous.channel().close();
        return session;
    }

    public Optional<DeviceSession> findByChannel(Channel channel) {
        String id = deviceByChannel.get(channel.id().asLongText());
        return id == null ? Optional.empty() : Optional.ofNullable(byDevice.get(id)).filter(s -> s.channel() == channel);
    }

    public SequenceDecision check(DeviceSession session, long sequence) {
        if (sequence == session.expectedSequence()) return SequenceDecision.EXPECTED;
        return sequence < session.expectedSequence() ? SequenceDecision.DUPLICATE : SequenceDecision.GAP;
    }

    public Optional<DeviceSession> markAccepted(Channel channel, long sequence) {
        String id = deviceByChannel.get(channel.id().asLongText());
        if (id == null) return Optional.empty();
        final DeviceSession[] updated = new DeviceSession[1];
        byDevice.computeIfPresent(id, (k,s) -> {
            if (s.channel() != channel) return s;
            updated[0] = s.accepted(sequence);
            return updated[0];
        });
        return Optional.ofNullable(updated[0]);
    }

    public Optional<DeviceSession> unregister(Channel channel) {
        String deviceId = deviceByChannel.remove(channel.id().asLongText());
        if (deviceId == null) return Optional.empty();
        final DeviceSession[] removed = new DeviceSession[1];
        byDevice.computeIfPresent(deviceId, (id, session) -> {
            if (session.channel() != channel) return session;
            removed[0] = session;
            return null;
        });
        return Optional.ofNullable(removed[0]);
    }

    public int activeCount() { return byDevice.size(); }
}
