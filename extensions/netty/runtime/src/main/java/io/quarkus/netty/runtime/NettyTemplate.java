package io.quarkus.netty.runtime;

import java.util.function.Supplier;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.quarkus.runtime.annotations.Template;

@Template
public class NettyTemplate {

    private static volatile NioEventLoopGroup bossGroup;
    private static volatile NioEventLoopGroup mainGroup;

    public EventLoopGroupSupplier createSupplier() {
        return new EventLoopGroupSupplier();
    }

    public void initBossGroup(EventLoopGroupSupplier supplier) {
        if (bossGroup == null) {
            bossGroup = new NioEventLoopGroup(1);
        }
        supplier.val = bossGroup;
    }

    public void initEventLoopGroup(EventLoopGroupSupplier supplier, IoConfig config) {
        if (mainGroup == null) {
            mainGroup = new NioEventLoopGroup(config.ioThreads.orElse(0));
        }
        supplier.val = mainGroup;
    }

    public static NioEventLoopGroup initBoosGroupForFailedStart() {
        return bossGroup = new NioEventLoopGroup(1);
    }

    public static NioEventLoopGroup initEventLoopForFailedStart(IoConfig ioConfig) {
        return mainGroup = new NioEventLoopGroup(ioConfig.ioThreads.orElse(0));
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
