package io.quarkus.netty.http.deployment.devmode;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.deployment.devmode.HotReplacementContext;
import io.quarkus.deployment.devmode.HotReplacementSetup;
import io.quarkus.deployment.devmode.ReplacementDebugPage;
import io.quarkus.netty.runtime.NettyHTTPTemplate;

public class NettyHTTPHotReplacementSetup extends SimpleChannelInboundHandler<HttpObject> implements HotReplacementSetup {

    private volatile long nextUpdate;
    private HotReplacementContext context;

    private static final long TWO_SECONDS = 2000;

    @Override
    public void setupHotDeployment(HotReplacementContext context) {
        this.context = context;
        NettyHTTPTemplate.addHotDeploymentHandler(this);
    }

    @Override
    public void handleFailedInitialStart() {
        NettyHTTPTemplate.startServerAfterFailedStart();
    }

    private void handleDeploymentProblem(ChannelHandlerContext ctx, HttpRequest request, final Throwable exception) {
        String bodyText = ReplacementDebugPage.generateHtml(exception);
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(),
                HttpResponseStatus.INTERNAL_SERVER_ERROR, Unpooled.copiedBuffer(bodyText, StandardCharsets.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        ctx.channel().writeAndFlush(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpRequest) {

            synchronized (this) {
                if (nextUpdate < System.currentTimeMillis()) {
                    context.doScan();
                    // we update at most once every 2s
                    nextUpdate = System.currentTimeMillis() + TWO_SECONDS;

                }
            }
            if (context.getDeploymentProblem() != null) {
                handleDeploymentProblem(ctx, (HttpRequest) msg, context.getDeploymentProblem());
                return;
            }
        }
        ctx.fireChannelRead(msg);
    }
}
