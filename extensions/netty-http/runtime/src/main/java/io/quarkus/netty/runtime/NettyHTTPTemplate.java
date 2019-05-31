package io.quarkus.netty.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

import javax.net.ssl.SSLContext;

import org.jboss.logging.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.quarkus.runtime.ExecutorTemplate;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.ThreadPoolConfig;
import io.quarkus.runtime.annotations.Template;
import io.quarkus.runtime.configuration.ConfigInstantiator;

@Template
public class NettyHTTPTemplate {
    private static final List<ChannelInboundHandler> hotDeploymentWrappers = new CopyOnWriteArrayList<>();

    private static final List<Channel> channels = new ArrayList<>();

    private static Logger log = Logger.getLogger(NettyHTTPTemplate.class);

    public static void startServerAfterFailedStart() {
        try {
            HttpConfig config = new HttpConfig();
            ConfigInstantiator.handleObject(config);

            ThreadPoolConfig threadPoolConfig = new ThreadPoolConfig();
            ConfigInstantiator.handleObject(threadPoolConfig);

            IoConfig ioConfig = new IoConfig();
            ConfigInstantiator.handleObject(ioConfig);


            ExecutorService service = ExecutorTemplate.createDevModeExecutorForFailedStart(threadPoolConfig);
            NioEventLoopGroup boosGroup = NettyTemplate.initBoosGroupForFailedStart();
            NioEventLoopGroup mainLoop = NettyTemplate.initEventLoopForFailedStart(ioConfig);
            //we can't really do
            doServerStart(config, LaunchMode.DEVELOPMENT, config.ssl.toSSLContext(), service, boosGroup, mainLoop);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void addHotDeploymentHandler(ChannelInboundHandler handlerWrapper) {
        hotDeploymentWrappers.add(handlerWrapper);
    }

    /**
     * Used for quarkus:run, where we want undertow to start very early in the process.
     * <p>
     * This enables recovery from errors on boot. In a normal boot undertow is one of the last things start, so there would
     * be no chance to use hot deployment to fix the error. In development mode we start Undertow early, so any error
     * on boot can be corrected via the hot deployment handler
     */
    private static void doServerStart(HttpConfig config, LaunchMode launchMode, SSLContext sslContext,
                                      ExecutorService service, NioEventLoopGroup boosGroup, NioEventLoopGroup workerGroup) {
        if (channels.isEmpty()) {
            int port = config.determinePort(launchMode);
            int sslPort = config.determineSslPort(launchMode);
            log.debugf("Starting HTTP on port %d", port);

            Channel ch = bootstrap(boosGroup, workerGroup)
                    .childHandler(new NettyRouterInitializer(worker, rootHandler, null))
                    .bind(listener.host, listener.port).sync().channel();

            channels.add(ch);


            if (sslContext != null) {
                log.debugf("Starting Undertow HTTPS listener on port %d", sslPort);
                builder.addHttpsListener(sslPort, config.host, sslContext);
            }
            undertow = builder
                    .build();
            undertow.start();
        }
    }

    private static ServerBootstrap bootstrap(NioEventLoopGroup bossGroup, NioEventLoopGroup workerGroup) {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        //TODO: socket options
        return new ServerBootstrap()
                .option(ChannelOption.ALLOCATOR, allocator)
                .childOption(ChannelOption.ALLOCATOR, allocator)
                // Requires EpollServerSocketChannel
//                .childOption(EpollChannelOption.TCP_CORK, socketOptions.get(UndertowOptions.CORK, true))
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class);
    }

}
