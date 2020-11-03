package io.quarkus.vertx.http.deployment.devmode;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.netty.runtime.virtual.VirtualChannel;
import io.quarkus.netty.runtime.virtual.VirtualServerChannel;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.devmode.DevConsoleFilter;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.Http1xServerConnection;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.impl.VertxHandler;

public class DevConsoleProcessor {

    protected static volatile ServerBootstrap virtualBootstrap;
    protected static volatile Vertx devConsoleVertx;

    public static void initializeVirtual() {
        if (virtualBootstrap != null) {
            return;
        }
        devConsoleVertx = Vertx.vertx();
        VertxInternal vertx = (VertxInternal) devConsoleVertx;
        virtualBootstrap = new ServerBootstrap();
        virtualBootstrap.group(vertx.getEventLoopGroup())
                .channel(VirtualServerChannel.class)
                .handler(new ChannelInitializer<VirtualServerChannel>() {
                    @Override
                    public void initChannel(VirtualServerChannel ch) throws Exception {
                        //ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                    }
                })
                .childHandler(new ChannelInitializer<VirtualChannel>() {
                    @Override
                    public void initChannel(VirtualChannel ch) throws Exception {
                        ContextInternal context = (ContextInternal) vertx
                                .createEventLoopContext(null, null, new JsonObject(),
                                        Thread.currentThread().getContextClassLoader());
                        VertxHandler<Http1xServerConnection> handler = VertxHandler.create(context, chctx -> {
                            Http1xServerConnection conn = new Http1xServerConnection(
                                    context.owner(),
                                    null,
                                    new HttpServerOptions(),
                                    chctx,
                                    context,
                                    "localhost",
                                    null);
                            conn.handler(new Handler<HttpServerRequest>() {
                                @Override
                                public void handle(HttpServerRequest event) {
                                    event.response().end("Hi from the deployment ClassLoader");
                                }
                            });
                            return conn;
                        });

                        ch.pipeline().addLast("handler", handler);
                    }
                });

        // Start the server.
        try {
            virtualBootstrap.bind(DevConsoleHttpHandler.QUARKUS_DEV_CONSOLE).sync();
        } catch (InterruptedException e) {
            throw new RuntimeException("failed to bind virtual http");
        }

    }

    @BuildStep()
    public RouteBuildItem setupBuildStep(LaunchModeBuildItem launchModeBuildItem) {
        if (!launchModeBuildItem.getLaunchMode().isDevOrTest()) {
            return null;
        }
        initializeVirtual();
        DevConsoleManager.registerHandler(new DevConsoleHttpHandler());
        return new RouteBuildItem("/@dev", new DevConsoleFilter());

    }

}
