package com.chronos.domain.world;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Map;

public record WorldState(String spaceId, Instant at, long lastEventSeq, Map<String, Object> state) {
    public WorldState {
        state = state == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(state));
    }

    public static Mutable mutable(String spaceId, Instant at, long lastEventSeq, Map<String, Object> initial) {
        return new Mutable(spaceId, at, lastEventSeq, new LinkedHashMap<>(initial == null ? Map.of() : initial));
    }

    public static final class Mutable {
        private final String spaceId;
        private Instant at;
        private long lastEventSeq;
        private final Map<String, Object> state;

        public Mutable(String spaceId, Instant at, long lastEventSeq, Map<String, Object> state) {
            this.spaceId = spaceId; this.at = at; this.lastEventSeq = lastEventSeq; this.state = state;
        }
        public Map<String, Object> state() { return state; }
        public void advance(Instant at, long seq) { this.at = at; this.lastEventSeq = seq; }
        public WorldState freeze() { return new WorldState(spaceId, at, lastEventSeq, state); }
    }
}
