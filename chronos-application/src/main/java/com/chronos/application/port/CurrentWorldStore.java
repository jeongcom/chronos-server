package com.chronos.application.port;

import com.chronos.domain.world.WorldState;
import java.util.Optional;

public interface CurrentWorldStore {
    Optional<WorldState> find(String spaceId);
    void save(WorldState state);
}
