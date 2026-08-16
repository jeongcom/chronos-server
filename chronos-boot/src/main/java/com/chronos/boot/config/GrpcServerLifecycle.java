package com.chronos.boot.config;

import com.chronos.grpc.ingest.ChronosIngestGrpcService;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class GrpcServerLifecycle implements SmartLifecycle {
    private final ChronosIngestGrpcService ingest; private final int port; private Server server; private volatile boolean running;
    public GrpcServerLifecycle(ChronosIngestGrpcService ingest,@Value("${chronos.grpc.port:9090}") int port){this.ingest=ingest;this.port=port;}
    @Override public void start(){
        try{server=NettyServerBuilder.forPort(port).addService(ingest).build().start();running=true;}
        catch(IOException e){throw new IllegalStateException("Failed to start gRPC on "+port,e);}
    }
    @Override public void stop(){
        if(server!=null){server.shutdown();try{server.awaitTermination(5,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
        running=false;
    }
    @Override public boolean isRunning(){return running;}
    @Override public int getPhase(){return Integer.MAX_VALUE;}
}
