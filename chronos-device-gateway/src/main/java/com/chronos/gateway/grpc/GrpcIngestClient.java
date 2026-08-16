package com.chronos.gateway.grpc;

import com.chronos.contract.v1.ChronosIngestServiceGrpc;
import com.chronos.contract.v1.PublishEventResponse;
import com.chronos.contract.v1.PublishEventsRequest;
import com.chronos.gateway.config.GatewayProperties;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class GrpcIngestClient {
    private final GatewayProperties properties;
    private ManagedChannel channel;
    private ChronosIngestServiceGrpc.ChronosIngestServiceBlockingStub stub;

    public GrpcIngestClient(GatewayProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void start() {
        var builder = ManagedChannelBuilder.forAddress(properties.core().host(), properties.core().grpcPort());
        if (properties.core().plaintext()) builder.usePlaintext();
        channel = builder.build();
        stub = ChronosIngestServiceGrpc.newBlockingStub(channel);
    }

    public List<PublishEventResponse> publish(List<PendingEvent> events) {
        var request = PublishEventsRequest.newBuilder();
        events.forEach(pending -> request.addEvents(pending.event()));
        return stub.withDeadlineAfter(properties.batch().rpcDeadlineMillis(), TimeUnit.MILLISECONDS)
                .publishEvents(request.build())
                .getResultsList();
    }

    @PreDestroy
    void stop() throws InterruptedException {
        if (channel != null) {
            channel.shutdown();
            if (!channel.awaitTermination(3, TimeUnit.SECONDS)) channel.shutdownNow();
        }
    }
}
