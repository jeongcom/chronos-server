package com.chronos.infrastructure.postgres;

import com.chronos.application.inbound.GetHistoricalWorldUseCase;
import com.chronos.application.port.*;
import com.chronos.domain.world.*;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;

@Service
public class HistoricalWorldService implements GetHistoricalWorldUseCase {
    private final SnapshotStore snapshots; private final EventStore events; private final WorldReplayEngine replay = new WorldReplayEngine();
    public HistoricalWorldService(SnapshotStore snapshots, EventStore events) { this.snapshots=snapshots; this.events=events; }
    @Override public WorldState get(String spaceId, Instant at) {
        var snap = snapshots.findLatest(spaceId, at);
        WorldState base = snap.map(s -> new WorldState(spaceId, s.snapshotAt(), s.lastEventSeq(), s.state()))
                .orElseGet(() -> new WorldState(spaceId, Instant.EPOCH, 0L, Map.of()));
        var following = events.find(spaceId, base.lastEventSeq(), at);
        WorldState result = replay.replay(base, following);
        return new WorldState(spaceId, at, result.lastEventSeq(), result.state());
    }
}
