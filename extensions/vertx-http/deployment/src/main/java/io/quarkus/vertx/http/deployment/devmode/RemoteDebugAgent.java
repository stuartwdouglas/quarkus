package io.quarkus.vertx.http.deployment.devmode;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingDeque;

import org.jboss.logging.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.quarkus.runtime.logging.LoggingSetupRecorder;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.impl.Http1xServerConnection;
import io.vertx.httpproxy.HttpProxy;

public class RemoteDebugAgent {

    static final byte[] END_SENTNEL = new byte[0];

    static final Logger log = Logger.getLogger(RemoteDebugAgent.class);

    public static void main() {
        try {
            LoggingSetupRecorder.handleFailedStart();
            Vertx vertx = Vertx.vertx();
            var client = vertx.createHttpClient(new HttpClientOptions().setDefaultHost("localhost").setDefaultPort(8081));
            var proxy = HttpProxy.reverseProxy(client);
            proxy.origin(8081, "localhost");
            HttpServer server = vertx.createHttpServer();
            server.requestHandler(new Handler<HttpServerRequest>() {
                @Override
                public void handle(HttpServerRequest event) {
                    String upgrade = event.getHeader("Upgrade");
                    if ("quarkus-remote-debug".equals(upgrade)) {
                        handleUpgrade(event);
                    } else {
                        proxy.handle(event);
                    }
                }
            }).listen(8080).toCompletionStage().toCompletableFuture().get();

            for (;;) {
                Thread.sleep(100000);
            }
        } catch (Exception e) {
            log.error("Problem running agent: ", e);
        }
    }

    private static void handleUpgrade(HttpServerRequest request) {

        try {
            Socket s = new Socket("localhost", 8000);

            HttpServerResponse response = request.response();
            response.headers().set(HttpHeaderNames.CONNECTION, "upgrade");
            Http1xServerConnection connection = (Http1xServerConnection) request.connection();
            ChannelHandlerContext context = connection.channelHandlerContext();
            ChannelHandler websocketChannelHandler = context.pipeline().get("webSocketExtensionHandler");
            if (websocketChannelHandler != null) {
                context.pipeline().remove(websocketChannelHandler);
            }

            response.setStatusCode(101).end(new Handler<AsyncResult<Void>>() {
                public void handle(AsyncResult<Void> event) {
                    Http1xServerConnection connection = (Http1xServerConnection) request.connection();
                    ChannelHandlerContext context = connection.channelHandlerContext();
                    ChannelPipeline p = context.pipeline();
                    p.remove("httpDecoder");
                    p.remove("httpEncoder");
                    p.remove("handler");
                    if (p.get(HttpObjectAggregator.class) != null) {
                        p.remove(HttpObjectAggregator.class);
                    }
                    if (p.get(HttpContentCompressor.class) != null) {
                        p.remove(HttpContentCompressor.class);
                    }
                    handleUpgradedConnection(context, s);
                }
            });
        } catch (IOException e) {
            log.error("Failed", e);
            request.response().setStatusCode(500).end("FAILED");
        }
    }

    private static void handleUpgradedConnection(ChannelHandlerContext context, Socket socket) {
        SocketData socketData = new SocketData(socket);
        context.pipeline().addFirst(new SimpleChannelInboundHandler() {
            @Override
            protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
                System.out.println("READ" + o);
                ByteBuf data = (ByteBuf) o;
                byte read[] = new byte[data.readableBytes()];
                data.readBytes(read);
                socketData.outbound.push(read);
            }
        });
        context.read();
        socketData.notificationListner = new Runnable() {
            @Override
            public void run() {
                System.out.println("NOTIFICATION " + socketData.inbound);
                while (!socketData.inbound.isEmpty()) {
                    ByteBuf b = socketData.inbound.poll();
                    context.writeAndFlush(b);
                }
            }
        };

        new Thread(socketData.readTask, "Debug read thread").start();
        new Thread(socketData.writeTask, "Debug write thread").start();
    }

    static class SocketData {
        final Socket socket;
        volatile Runnable notificationListner;
        final LinkedBlockingDeque<ByteBuf> inbound = new LinkedBlockingDeque<>();
        final LinkedBlockingDeque<byte[]> outbound = new LinkedBlockingDeque<>();

        Runnable readTask = new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] buf = new byte[1024];
                    for (;;) {
                        int read = socket.getInputStream().read(buf);
                        if (read < 0) {
                            System.out.println("Connection closed, exiting read side");
                            return;
                        }
                        System.out.println("READ data" + read + " " + notificationListner);
                        inbound.push(Unpooled.copiedBuffer(buf, 0, read));
                        if (notificationListner != null) {
                            notificationListner.run();
                        }
                    }
                } catch (IOException e) {
                    log.error("Socket connection failed", e);
                }
            }
        };
        Runnable writeTask = new Runnable() {
            @Override
            public void run() {
                try {
                    for (;;) {
                        byte[] data = outbound.take();
                        if (data == END_SENTNEL) {
                            return;
                        }
                        System.out.println("WRITING: " + data.length);
                        socket.getOutputStream().write(data);
                    }
                } catch (IOException | InterruptedException e) {
                    log.error("Socket connection failed", e);
                }
            }
        };

        SocketData(Socket socket) {
            this.socket = socket;
        }
    }

}
