package io.quarkus.netty.runtime;

import java.util.function.Supplier;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.quarkus.runtime.annotations.Template;

@Template
public class NettyTemplate {

    public EventLoopGroupSupplier createSupplier() {
        return new EventLoopGroupSupplier();
    }

    public void initBossGroup(EventLoopGroupSupplier supplier) {
        supplier.val = new NioEventLoopGroup(1);
    }

    public void initEventLoopGroup(EventLoopGroupSupplier supplier, IoConfig config) {
        supplier.val = new NioEventLoopGroup(config);
    }

    public Supplier<EventLoopGroup> createEventLoop(int nThreads) {
        return new Supplier<EventLoopGroup>() {

            volatile EventLoopGroup val;

            @Override
            public EventLoopGroup get() {
                if (val == null) {
                    synchronized (this) {
                        if (val == null) {
                            val = new NioEventLoopGroup(nThreads);
                        }
                    }
                }
                return val;
            }
        };
    }

    public static class EventLoopGroupSupplier implements Supplier<EventLoopGroup> {


        volatile EventLoopGroup val;

        public void set(EventLoopGroup group) {
            this.val = group;
        }

        @Override
        public EventLoopGroup get() {
            return null;
        }
    }
}
