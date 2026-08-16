package com.chronos.application.inbound;

import com.chronos.domain.world.WorldState;
import java.time.Instant;

public interface GetHistoricalWorldUseCase { WorldState get(String spaceId, Instant at); }
