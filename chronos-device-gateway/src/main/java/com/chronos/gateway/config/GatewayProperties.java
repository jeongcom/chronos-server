package com.chronos.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chronos.gateway")
public record GatewayProperties(
        Tcp tcp,
        Core core,
        Batch batch) {

    public record Tcp(String host, int port, int maxFrameBytes, int readIdleSeconds) {}
    public record Core(String host, int grpcPort, boolean plaintext) {}
    public record Batch(int maxEvents, long flushMillis, int queueCapacity, long rpcDeadlineMillis) {}
}
