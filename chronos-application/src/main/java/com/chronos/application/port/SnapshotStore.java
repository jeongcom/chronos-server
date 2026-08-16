package com.chronos.application.port;

import com.chronos.domain.snapshot.WorldSnapshot;
import java.time.Instant;
import java.util.Optional;

public interface SnapshotStore { Optional<WorldSnapshot> findLatest(String spaceId, Instant atOrBefore); }
