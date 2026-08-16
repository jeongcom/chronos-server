package com.chronos.application.inbound;

import com.chronos.domain.event.ChronosEvent;

public interface IngestEventUseCase {
    IngestResult ingest(ChronosEvent event);
    record IngestResult(Status status, long eventSeq, String message) {
        public enum Status { ACCEPTED, DUPLICATE, INVALID, REJECTED }
    }
}
