package com.chronos.infrastructure.postgres;

import com.chronos.application.inbound.IngestEventUseCase;
import com.chronos.application.port.*;
import com.chronos.domain.event.ChronosEvent;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionalIngestService implements IngestEventUseCase {
    public static final String EVENT_TOPIC = "chronos.events.v1";
    private final EventStore eventStore; private final OutboxPort outbox;
    public TransactionalIngestService(EventStore eventStore, OutboxPort outbox) { this.eventStore=eventStore; this.outbox=outbox; }

    @Override @Transactional
    public IngestResult ingest(ChronosEvent event) {
        try {
            var stored = eventStore.append(event);
            outbox.enqueue(EVENT_TOPIC, event.spaceId(), stored);
            return new IngestResult(IngestResult.Status.ACCEPTED, stored.eventSeq(), "accepted");
        } catch (DuplicateKeyException e) {
            var duplicate = eventStore.findById(event.eventId());
            if (duplicate.isEmpty() && event.sourceSequence() != null) {
                duplicate = eventStore.findBySourceSequence(event.sourceId(), event.sourceSequence());
            }
            return duplicate
                    .map(found -> new IngestResult(IngestResult.Status.DUPLICATE, found.eventSeq(), "duplicate"))
                    .orElseGet(() -> new IngestResult(IngestResult.Status.DUPLICATE, 0, "duplicate"));
        } catch (IllegalArgumentException e) {
            return new IngestResult(IngestResult.Status.INVALID, 0, e.getMessage());
        }
    }
}
