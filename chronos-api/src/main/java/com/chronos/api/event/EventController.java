package com.chronos.api.event;

import com.chronos.application.inbound.IngestEventUseCase;
import com.chronos.domain.event.ChronosEvent;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {
    private final IngestEventUseCase ingest;
    public EventController(IngestEventUseCase ingest){this.ingest=ingest;}
    @PostMapping @ResponseStatus(HttpStatus.ACCEPTED)
    public IngestEventUseCase.IngestResult publish(@RequestBody ChronosEvent event){return ingest.ingest(event);}
}
