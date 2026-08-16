package com.chronos.infrastructure.redis;

import com.chronos.application.inbound.GetCurrentWorldUseCase;
import com.chronos.application.port.CurrentWorldStore;
import com.chronos.domain.world.WorldState;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class CurrentWorldService implements GetCurrentWorldUseCase {
    private final CurrentWorldStore store;
    public CurrentWorldService(CurrentWorldStore store){this.store=store;}
    @Override public Optional<WorldState> get(String spaceId){return store.find(spaceId);}
}
