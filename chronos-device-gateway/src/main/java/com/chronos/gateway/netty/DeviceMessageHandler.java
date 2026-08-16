package com.chronos.gateway.netty;

import com.chronos.gateway.grpc.BatchIngestDispatcher;
import com.chronos.gateway.grpc.ChronosEventFactory;
import com.chronos.gateway.grpc.GrpcDeviceRegistryClient;
import com.chronos.gateway.protocol.*;
import com.chronos.gateway.session.DeviceSession;
import com.chronos.gateway.session.DeviceSessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@io.netty.channel.ChannelHandler.Sharable
public class DeviceMessageHandler extends SimpleChannelInboundHandler<DeviceFrame> {
    private static final Logger log = LoggerFactory.getLogger(DeviceMessageHandler.class);
    private final ChronosEventFactory eventFactory;
    private final BatchIngestDispatcher dispatcher;
    private final DeviceSessionManager sessions;
    private final GrpcDeviceRegistryClient registry;
    private final JsonMapper json;

    public DeviceMessageHandler(ChronosEventFactory eventFactory, BatchIngestDispatcher dispatcher,
            DeviceSessionManager sessions, GrpcDeviceRegistryClient registry, JsonMapper json) {
        this.eventFactory=eventFactory; this.dispatcher=dispatcher; this.sessions=sessions; this.registry=registry; this.json=json;
    }

    @Override protected void channelRead0(ChannelHandlerContext ctx, DeviceFrame frame) {
        if (frame.messageType() == DeviceMessageType.AUTHENTICATE) { authenticate(ctx, frame); return; }
        DeviceSession session = sessions.findByChannel(ctx.channel()).orElse(null);
        if (session == null || !session.deviceId().equals(frame.deviceId())) {
            ctx.writeAndFlush(new DeviceAck(frame.sequence(), DeviceAck.REJECTED, 0)); return;
        }
        var decision = sessions.check(session, frame.sequence());
        if (decision == DeviceSessionManager.SequenceDecision.GAP) {
            log.warn("Sequence gap device={} expected={} received={}", session.deviceId(), session.expectedSequence(), frame.sequence());
            ctx.writeAndFlush(new RetransmitRequest(session.expectedSequence(), frame.sequence()-1));
            return;
        }
        if (decision == DeviceSessionManager.SequenceDecision.DUPLICATE) {
            ctx.writeAndFlush(new DeviceAck(frame.sequence(), DeviceAck.DUPLICATE, 0)); return;
        }

        ctx.channel().config().setAutoRead(false);
        dispatcher.submit(eventFactory.create(frame)).whenComplete((response,error) -> ctx.executor().execute(() -> {
            try {
                if (!ctx.channel().isActive()) return;
                if (error != null) { ctx.writeAndFlush(new DeviceAck(frame.sequence(),DeviceAck.REJECTED,0)); return; }
                int status = switch(response.getStatus()) {
                    case ACCEPTED -> DeviceAck.ACCEPTED; case DUPLICATE -> DeviceAck.DUPLICATE;
                    case INVALID -> DeviceAck.INVALID; default -> DeviceAck.REJECTED;
                };
                if (status == DeviceAck.ACCEPTED || status == DeviceAck.DUPLICATE) {
                    sessions.markAccepted(ctx.channel(), frame.sequence()).ifPresent(s ->
                        CompletableFuture.runAsync(() -> { try { registry.heartbeat(s); } catch(Exception e) { log.warn("Heartbeat update failed {}", s.deviceId()); } }));
                }
                ctx.writeAndFlush(new DeviceAck(frame.sequence(),status,response.getEventSeq()));
            } finally { if (ctx.channel().isActive()) { ctx.channel().config().setAutoRead(true); ctx.read(); } }
        }));
    }

    private void authenticate(ChannelHandlerContext ctx, DeviceFrame frame) {
        if (sessions.findByChannel(ctx.channel()).isPresent()) { ctx.writeAndFlush(new DeviceAck(frame.sequence(),DeviceAck.DUPLICATE,0)); return; }
        ctx.channel().config().setAutoRead(false);
        CompletableFuture.runAsync(() -> {
            try {
                @SuppressWarnings("unchecked") Map<String,Object> payload = json.readValue(frame.payloadJson(), Map.class);
                String secret = String.valueOf(payload.getOrDefault("secret", ""));
                var r = registry.authenticate(frame.deviceId(), secret, ctx.channel().id().asLongText());
                ctx.executor().execute(() -> {
                    try {
                        if (r.getAuthenticated()) {
                            sessions.registerAuthenticated(frame.deviceId(), r.getSpaceId(), r.getExpectedSequence(), ctx.channel());
                            ctx.writeAndFlush(new DeviceAck(frame.sequence(),DeviceAck.ACCEPTED,r.getExpectedSequence()));
                        } else {
                            log.warn("Authentication rejected device={} reason={}", frame.deviceId(), r.getMessage());
                            ctx.writeAndFlush(new DeviceAck(frame.sequence(),DeviceAck.REJECTED,0)).addListener(f -> ctx.close());
                        }
                    } finally { if (ctx.channel().isActive()) { ctx.channel().config().setAutoRead(true); ctx.read(); } }
                });
            } catch(Exception e) {
                ctx.executor().execute(() -> ctx.writeAndFlush(new DeviceAck(frame.sequence(),DeviceAck.INVALID,0)).addListener(f -> ctx.close()));
            }
        });
    }

    @Override public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        sessions.unregister(ctx.channel()).ifPresent(s -> CompletableFuture.runAsync(() -> registry.disconnect(s,"CHANNEL_INACTIVE")));
        super.channelInactive(ctx);
    }
    @Override public void userEventTriggered(ChannelHandlerContext ctx,Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) { sessions.unregister(ctx.channel()).ifPresent(s -> CompletableFuture.runAsync(() -> registry.disconnect(s,"READ_IDLE"))); ctx.close(); return; }
        super.userEventTriggered(ctx,evt);
    }
    @Override public void exceptionCaught(ChannelHandlerContext ctx,Throwable cause) { log.warn("Device channel error {}",cause.toString()); ctx.close(); }
}
