package com.chronos.domain.event;

public record StoredEvent(long eventSeq, ChronosEvent event) {}
