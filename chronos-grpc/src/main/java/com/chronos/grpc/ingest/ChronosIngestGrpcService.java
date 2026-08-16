package com.chronos.grpc.ingest;

import com.chronos.application.inbound.IngestEventUseCase;
import com.chronos.contract.v1.*;
import com.chronos.grpc.mapper.GrpcEventMapper;
import tools.jackson.databind.json.JsonMapper;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

@Component
public class ChronosIngestGrpcService extends ChronosIngestServiceGrpc.ChronosIngestServiceImplBase {
    private final IngestEventUseCase ingest; private final JsonMapper json;
    public ChronosIngestGrpcService(IngestEventUseCase ingest,JsonMapper json){this.ingest=ingest;this.json=json;}

    @Override public void publishEvent(PublishEventRequest request, StreamObserver<PublishEventResponse> observer){
        try{observer.onNext(toProto(ingest.ingest(GrpcEventMapper.toDomain(request.getEvent(),json))));observer.onCompleted();}
        catch(Exception e){observer.onNext(PublishEventResponse.newBuilder().setStatus(PublishStatus.INVALID).setMessage(e.getMessage()).build());observer.onCompleted();}
    }
    @Override public void publishEvents(PublishEventsRequest request,StreamObserver<PublishEventsResponse> observer){
        var b=PublishEventsResponse.newBuilder();
        request.getEventsList().forEach(e->{try{b.addResults(toProto(ingest.ingest(GrpcEventMapper.toDomain(e,json))));}
            catch(Exception ex){b.addResults(PublishEventResponse.newBuilder().setStatus(PublishStatus.INVALID).setMessage(ex.getMessage()).build());}});
        observer.onNext(b.build());observer.onCompleted();
    }
    private PublishEventResponse toProto(IngestEventUseCase.IngestResult r){
        PublishStatus s=switch(r.status()){case ACCEPTED->PublishStatus.ACCEPTED;case DUPLICATE->PublishStatus.DUPLICATE;case INVALID->PublishStatus.INVALID;case REJECTED->PublishStatus.REJECTED;};
        return PublishEventResponse.newBuilder().setStatus(s).setEventSeq(r.eventSeq()).setMessage(r.message()==null?"":r.message()).build();
    }
}
