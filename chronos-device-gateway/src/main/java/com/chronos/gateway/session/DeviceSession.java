package com.chronos.gateway.session;

import io.netty.channel.Channel;
import java.net.SocketAddress;
import java.time.Instant;

public record DeviceSession(
        String deviceId,
        Channel channel,
        SocketAddress remoteAddress,
        Instant connectedAt,
        Instant lastSeenAt) {

    public DeviceSession touch() {
        return new DeviceSession(deviceId, channel, remoteAddress, connectedAt, Instant.now());
    }
}
