package com.chronos.infrastructure.kafka;

import com.chronos.application.port.CurrentWorldStore;
import com.chronos.domain.event.StoredEvent;
import com.chronos.domain.world.*;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Map;

@Component
public class WorldProjector {
    private final CurrentWorldStore current; private final JsonMapper json; private final WorldReplayEngine replay=new WorldReplayEngine();
    public WorldProjector(CurrentWorldStore current,JsonMapper json){this.current=current;this.json=json;}

    @KafkaListener(topics="chronos.events.v1", groupId="chronos-world-projector-v1")
    public void onEvent(String message){
        try{
            StoredEvent stored=json.readValue(message,StoredEvent.class);
            String space=stored.event().spaceId();
            WorldState base=current.find(space).orElseGet(()->new WorldState(space,Instant.EPOCH,0,Map.of()));
            if(stored.eventSeq()<=base.lastEventSeq()) return;
            WorldState next=replay.replay(base,java.util.List.of(stored));
            current.save(next);
        }catch(Exception e){throw new IllegalStateException("World projection failed",e);}
    }
}
