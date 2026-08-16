package com.chronos.gateway.grpc;

import com.chronos.contract.v1.ChronosEvent;
import com.chronos.contract.v1.PublishEventResponse;

import java.util.concurrent.CompletableFuture;

public record PendingEvent(
        ChronosEvent event,
        CompletableFuture<PublishEventResponse> result) {
}
