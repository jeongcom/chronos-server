package com.chronos.application.port;

import com.chronos.domain.event.ChronosEvent;
import com.chronos.domain.event.StoredEvent;
import java.time.Instant;
import java.util.*;

public interface EventStore {
    StoredEvent append(ChronosEvent event);
    Optional<StoredEvent> findById(UUID eventId);
    Optional<StoredEvent> findBySourceSequence(String sourceId, long sourceSequence);
    List<StoredEvent> find(String spaceId, long afterEventSeq, Instant untilInclusive);
}
