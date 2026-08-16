package com.chronos.domain.world;

import com.chronos.domain.event.StoredEvent;
import java.util.*;

public final class WorldReplayEngine {
    public WorldState replay(WorldState base, List<StoredEvent> events) {
        var world = WorldState.mutable(base.spaceId(), base.at(), base.lastEventSeq(), base.state());
        for (StoredEvent stored : events) apply(world, stored);
        return world.freeze();
    }

    @SuppressWarnings("unchecked")
    public void apply(WorldState.Mutable world, StoredEvent stored) {
        var e = stored.event();
        var s = world.state();
        switch (e.eventType()) {
            case "DEVICE.TEMPERATURE.CHANGED" -> s.put("temperature", e.payload().get("temperature"));
            case "DOOR.OPENED" -> nestedMap(s, "doors").put(e.sourceId(), "OPEN");
            case "DOOR.CLOSED" -> nestedMap(s, "doors").put(e.sourceId(), "CLOSED");
            case "LIGHT.TURNED_ON" -> nestedMap(s, "lights").put(e.sourceId(), "ON");
            case "LIGHT.TURNED_OFF" -> nestedMap(s, "lights").put(e.sourceId(), "OFF");
            case "PERSON.ENTERED" -> stringSet(s, "people").add(String.valueOf(e.payload().getOrDefault("personId", e.sourceId())));
            case "PERSON.EXITED" -> stringSet(s, "people").remove(String.valueOf(e.payload().getOrDefault("personId", e.sourceId())));
            default -> s.put("lastEventType", e.eventType());
        }
        s.put("lastSourceId", e.sourceId());
        s.put("lastOccurredAt", e.occurredAt().toString());
        world.advance(e.occurredAt(), stored.eventSeq());
    }

    private Map<String, Object> nestedMap(Map<String, Object> root, String key) {
        Object existing = root.get(key);
        if (existing instanceof Map<?, ?> map) return (Map<String, Object>) map;
        Map<String, Object> created = new LinkedHashMap<>(); root.put(key, created); return created;
    }

    private Set<String> stringSet(Map<String, Object> root, String key) {
        Object existing = root.get(key);
        if (existing instanceof Set<?> set) return (Set<String>) set;
        if (existing instanceof Collection<?> c) {
            Set<String> converted = new LinkedHashSet<>(); c.forEach(v -> converted.add(String.valueOf(v)));
            root.put(key, converted); return converted;
        }
        Set<String> created = new LinkedHashSet<>(); root.put(key, created); return created;
    }
}
