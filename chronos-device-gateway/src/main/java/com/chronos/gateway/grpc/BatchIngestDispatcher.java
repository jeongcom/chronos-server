package com.chronos.gateway.grpc;

import com.chronos.contract.v1.ChronosEvent;
import com.chronos.contract.v1.PublishEventResponse;
import com.chronos.contract.v1.PublishStatus;
import com.chronos.gateway.config.GatewayProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class BatchIngestDispatcher {
    private final GatewayProperties properties;
    private final GrpcIngestClient client;
    private final BlockingQueue<PendingEvent> queue;
    private final AtomicBoolean running = new AtomicBoolean();
    private Thread worker;

    public BatchIngestDispatcher(GatewayProperties properties, GrpcIngestClient client) {
        this.properties = properties;
        this.client = client;
        this.queue = new ArrayBlockingQueue<>(properties.batch().queueCapacity());
    }

    public CompletableFuture<PublishEventResponse> submit(ChronosEvent event) {
        var future = new CompletableFuture<PublishEventResponse>();
        if (!queue.offer(new PendingEvent(event, future))) {
            future.complete(rejected("gateway ingest queue full"));
        }
        return future;
    }

    @PostConstruct
    void start() {
        running.set(true);
        worker = Thread.ofPlatform().name("chronos-ingest-batch").daemon(true).start(this::runLoop);
    }

    @PreDestroy
    void stop() throws InterruptedException {
        running.set(false);
        if (worker != null) {
            worker.interrupt();
            worker.join(2000);
        }
        PendingEvent pending;
        while ((pending = queue.poll()) != null) pending.result().complete(rejected("gateway stopping"));
    }

    private void runLoop() {
        var batch = new ArrayList<PendingEvent>(properties.batch().maxEvents());
        while (running.get()) {
            try {
                PendingEvent first = queue.poll(properties.batch().flushMillis(), TimeUnit.MILLISECONDS);
                if (first == null) continue;
                batch.add(first);
                queue.drainTo(batch, properties.batch().maxEvents() - 1);
                sendWithRetry(List.copyOf(batch));
                batch.clear();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                batch.forEach(p -> p.result().completeExceptionally(e));
                batch.clear();
            }
        }
    }

    private void sendWithRetry(List<PendingEvent> batch) {
        Exception last = null;
        long delay = 100;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                List<PublishEventResponse> responses = client.publish(batch);
                if (responses.size() != batch.size()) {
                    throw new IllegalStateException("gRPC result count mismatch: " + responses.size() + "/" + batch.size());
                }
                for (int i = 0; i < batch.size(); i++) batch.get(i).result().complete(responses.get(i));
                return;
            } catch (Exception e) {
                last = e;
                try { Thread.sleep(delay); } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
                delay *= 2;
            }
        }
        Exception failure = last != null ? last : new IllegalStateException("gRPC publish failed");
        batch.forEach(p -> p.result().completeExceptionally(failure));
    }

    private static PublishEventResponse rejected(String message) {
        return PublishEventResponse.newBuilder().setStatus(PublishStatus.REJECTED).setMessage(message).build();
    }
}
