package com.chronos.gateway.grpc;

import com.chronos.contract.v1.*;
import com.chronos.gateway.config.GatewayProperties;
import com.chronos.gateway.session.DeviceSession;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class GrpcDeviceRegistryClient {
    private final GatewayProperties properties;
    private ManagedChannel channel;
    private DeviceRegistryServiceGrpc.DeviceRegistryServiceBlockingStub stub;

    public GrpcDeviceRegistryClient(GatewayProperties properties) { this.properties = properties; }

    @PostConstruct void start() {
        var b = ManagedChannelBuilder.forAddress(properties.core().host(), properties.core().grpcPort());
        if (properties.core().plaintext()) b.usePlaintext();
        channel = b.build();
        stub = DeviceRegistryServiceGrpc.newBlockingStub(channel);
    }

    public AuthenticateDeviceResponse authenticate(String deviceId, String secret, String connectionId) {
        return stub.withDeadlineAfter(properties.batch().rpcDeadlineMillis(), TimeUnit.MILLISECONDS)
            .authenticate(AuthenticateDeviceRequest.newBuilder().setDeviceId(deviceId).setSecret(secret)
                .setGatewayId(properties.id()).setConnectionId(connectionId).build());
    }

    public boolean heartbeat(DeviceSession s) {
        return stub.withDeadlineAfter(properties.batch().rpcDeadlineMillis(), TimeUnit.MILLISECONDS)
            .heartbeat(DeviceHeartbeatRequest.newBuilder().setDeviceId(s.deviceId()).setGatewayId(properties.id())
                .setConnectionId(s.connectionId()).setLastSequence(s.lastAcceptedSequence()).build()).getAccepted();
    }

    public void disconnect(DeviceSession s, String reason) {
        try {
            stub.withDeadlineAfter(properties.batch().rpcDeadlineMillis(), TimeUnit.MILLISECONDS)
                .disconnect(DeviceDisconnectRequest.newBuilder().setDeviceId(s.deviceId()).setGatewayId(properties.id())
                    .setConnectionId(s.connectionId()).setReason(reason == null ? "DISCONNECTED" : reason).build());
        } catch (Exception ignored) { }
    }

    @PreDestroy void stop() throws InterruptedException {
        if (channel != null) { channel.shutdown(); if (!channel.awaitTermination(2, TimeUnit.SECONDS)) channel.shutdownNow(); }
    }
}
