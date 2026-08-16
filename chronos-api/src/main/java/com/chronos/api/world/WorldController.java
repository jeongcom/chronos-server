package com.chronos.api.world;

import com.chronos.application.inbound.*;
import com.chronos.domain.world.WorldState;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/world")
public class WorldController {
    private final GetHistoricalWorldUseCase historical; private final GetCurrentWorldUseCase current;
    public WorldController(GetHistoricalWorldUseCase historical, GetCurrentWorldUseCase current){this.historical=historical;this.current=current;}

    @GetMapping("/{spaceId}/state")
    public WorldState state(@PathVariable String spaceId,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant at){
        return historical.get(spaceId,at);
    }

    @GetMapping("/{spaceId}/current")
    public ResponseEntity<WorldState> current(@PathVariable String spaceId){
        return current.get(spaceId).map(ResponseEntity::ok).orElseGet(()->ResponseEntity.notFound().build());
    }
}
