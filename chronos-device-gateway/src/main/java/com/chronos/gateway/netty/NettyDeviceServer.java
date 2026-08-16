package com.chronos.gateway.netty;

import com.chronos.gateway.config.GatewayProperties;
import com.chronos.gateway.protocol.AckEncoder;
import com.chronos.gateway.protocol.DeviceFrameDecoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class NettyDeviceServer implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(NettyDeviceServer.class);

    private final GatewayProperties properties;
    private final DeviceMessageHandler handler;
    private final AtomicBoolean running = new AtomicBoolean();
    private EventLoopGroup boss;
    private EventLoopGroup workers;
    private Channel serverChannel;

    public NettyDeviceServer(GatewayProperties properties, DeviceMessageHandler handler) {
        this.properties = properties;
        this.handler = handler;
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) return;
        boss = new NioEventLoopGroup(1);
        workers = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(boss, workers)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast("idle", new IdleStateHandler(properties.tcp().readIdleSeconds(), 0, 0, TimeUnit.SECONDS))
                                    .addLast("frameDecoder", new DeviceFrameDecoder(properties.tcp().maxFrameBytes()))
                                    .addLast("ackEncoder", new AckEncoder())
                                    .addLast("deviceHandler", handler);
                        }
                    });

            serverChannel = bootstrap.bind(properties.tcp().host(), properties.tcp().port()).sync().channel();
            log.info("CHRONOS Device Gateway listening on {}:{}", properties.tcp().host(), properties.tcp().port());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stop();
            throw new IllegalStateException("Netty gateway startup interrupted", e);
        } catch (RuntimeException e) {
            stop();
            throw e;
        }
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) return;
        if (serverChannel != null) serverChannel.close().awaitUninterruptibly();
        if (workers != null) workers.shutdownGracefully().awaitUninterruptibly();
        if (boss != null) boss.shutdownGracefully().awaitUninterruptibly();
        log.info("CHRONOS Device Gateway stopped");
    }

    @Override public boolean isRunning() { return running.get(); }
    @Override public boolean isAutoStartup() { return true; }
    @Override public int getPhase() { return 0; }
}
