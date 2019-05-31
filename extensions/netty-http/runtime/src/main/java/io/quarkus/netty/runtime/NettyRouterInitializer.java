/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkus.netty.runtime;

import java.util.concurrent.ExecutorService;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslHandler;
import io.undertow.server.HttpHandler;

public class NettyRouterInitializer extends ChannelInitializer<SocketChannel> {

    private final ExecutorService blockingExecutor;
    private final HttpHandler rootHandler;
    private final SSLContext sslCtx;
    private

    public NettyRouterInitializer(ExecutorService blockingExecutor, HttpHandler rootHandler, SSLContext sslCtx) {
        this.blockingExecutor = blockingExecutor;
        this.rootHandler = rootHandler;
        this.sslCtx = sslCtx;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        SSLEngine engine = null;
        if (sslCtx != null) {
            SSLEngine sslEngine = sslCtx.createSSLEngine();
            sslEngine.setUseClientMode(false);
            SslHandler sslHandler = new SslHandler(sslEngine);
            engine = sslHandler.engine();
            p.addLast(sslHandler);
        }
        p.addLast(new HttpServerCodec());
        p.addLast(new NettyHttpServerHandler(blockingExecutor, rootHandler, engine));
    }
}
