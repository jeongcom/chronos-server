package com.chronos.gateway.session;

import io.netty.channel.Channel;
import java.net.SocketAddress;
import java.time.Instant;

public record DeviceSession(
        String deviceId,
        String spaceId,
        String connectionId,
        Channel channel,
        SocketAddress remoteAddress,
        Instant connectedAt,
        Instant lastSeenAt,
        long expectedSequence,
        long lastAcceptedSequence) {

    public DeviceSession touch() {
        return new DeviceSession(deviceId, spaceId, connectionId, channel, remoteAddress,
                connectedAt, Instant.now(), expectedSequence, lastAcceptedSequence);
    }
    public DeviceSession accepted(long sequence) {
        return new DeviceSession(deviceId, spaceId, connectionId, channel, remoteAddress,
                connectedAt, Instant.now(), sequence + 1, sequence);
    }
}
