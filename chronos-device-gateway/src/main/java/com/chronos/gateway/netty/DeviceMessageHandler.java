package com.chronos.gateway.netty;

import com.chronos.gateway.grpc.BatchIngestDispatcher;
import com.chronos.gateway.grpc.ChronosEventFactory;
import com.chronos.gateway.protocol.DeviceAck;
import com.chronos.gateway.protocol.DeviceFrame;
import com.chronos.gateway.session.DeviceSessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@io.netty.channel.ChannelHandler.Sharable
public class DeviceMessageHandler extends SimpleChannelInboundHandler<DeviceFrame> {
    private static final Logger log = LoggerFactory.getLogger(DeviceMessageHandler.class);

    private final ChronosEventFactory eventFactory;
    private final BatchIngestDispatcher dispatcher;
    private final DeviceSessionManager sessions;

    public DeviceMessageHandler(ChronosEventFactory eventFactory,
                                BatchIngestDispatcher dispatcher,
                                DeviceSessionManager sessions) {
        this.eventFactory = eventFactory;
        this.dispatcher = dispatcher;
        this.sessions = sessions;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DeviceFrame frame) {
        sessions.register(frame.deviceId(), ctx.channel());
        sessions.touch(frame.deviceId(), ctx.channel());

        var event = eventFactory.create(frame);
        dispatcher.submit(event).whenComplete((response, error) -> {
            if (!ctx.channel().isActive()) return;
            if (error != null) {
                log.warn("Ingest failed device={} seq={}: {}", frame.deviceId(), frame.sequence(), error.toString());
                ctx.executor().execute(() -> ctx.writeAndFlush(new DeviceAck(frame.sequence(), DeviceAck.REJECTED, 0)));
                return;
            }

            int status = switch (response.getStatus()) {
                case ACCEPTED -> DeviceAck.ACCEPTED;
                case DUPLICATE -> DeviceAck.DUPLICATE;
                case INVALID -> DeviceAck.INVALID;
                default -> DeviceAck.REJECTED;
            };
            ctx.executor().execute(() -> ctx.writeAndFlush(new DeviceAck(frame.sequence(), status, response.getEventSeq())));
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        sessions.unregister(ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            log.info("Closing idle device connection {}", ctx.channel().remoteAddress());
            ctx.close();
            return;
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.warn("Device channel error remote={}: {}", ctx.channel().remoteAddress(), cause.toString());
        ctx.close();
    }
}
