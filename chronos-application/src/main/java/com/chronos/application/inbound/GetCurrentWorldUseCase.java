package com.chronos.application.inbound;

import com.chronos.domain.world.WorldState;
import java.util.Optional;

public interface GetCurrentWorldUseCase { Optional<WorldState> get(String spaceId); }
