package com.chronos.gateway.protocol;

public record RetransmitRequest(long fromSequence, long toSequence) {}
