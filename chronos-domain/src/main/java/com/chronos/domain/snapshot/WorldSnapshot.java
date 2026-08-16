package com.chronos.domain.snapshot;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record WorldSnapshot(UUID snapshotId, String spaceId, Instant snapshotAt, long lastEventSeq, Map<String,Object> state) {}
