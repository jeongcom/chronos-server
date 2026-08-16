package com.chronos.grpc.mapper;

import com.chronos.domain.event.ChronosEvent;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.*;

public final class GrpcEventMapper {
    private GrpcEventMapper(){}
    public static ChronosEvent toDomain(com.chronos.contract.v1.ChronosEvent p,JsonMapper json){
        try{
            Map<String,Object> payload=p.getPayloadJson().isBlank()?Map.of():json.readValue(p.getPayloadJson(),new TypeReference<>(){});
            return new ChronosEvent(UUID.fromString(p.getEventId()),p.getEventType(),p.getSchemaVersion(),p.getSourceType(),p.getSourceId(),
                p.hasSourceSequence()?p.getSourceSequence():null,p.getSpaceId(),toInstant(p.getOccurredAt()),toInstant(p.getReceivedAt()),
                p.hasCorrelationId()?UUID.fromString(p.getCorrelationId()):null,p.hasCausationId()?UUID.fromString(p.getCausationId()):null,
                p.getConfidence(),payload);
        }catch(Exception e){throw new IllegalArgumentException("Invalid gRPC event: "+e.getMessage(),e);}
    }
    private static Instant toInstant(com.google.protobuf.Timestamp t){return Instant.ofEpochSecond(t.getSeconds(),t.getNanos());}
}
