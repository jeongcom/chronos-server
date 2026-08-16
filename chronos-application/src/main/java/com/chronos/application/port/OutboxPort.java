package com.chronos.application.port;

import com.chronos.domain.event.StoredEvent;

public interface OutboxPort { void enqueue(String topic, String eventKey, StoredEvent event); }
