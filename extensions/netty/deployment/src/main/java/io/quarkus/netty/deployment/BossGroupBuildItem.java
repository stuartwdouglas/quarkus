package io.quarkus.netty.deployment;

import java.util.function.Supplier;

import io.netty.channel.EventLoopGroup;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The Netty boss event loop group that handles accepts
 */
public final class BossGroupBuildItem extends SimpleBuildItem {

    private final EventLoopGroup group;

    public BossGroupBuildItem(EventLoopGroup group) {
        this.group = group;
    }

    public EventLoopGroup getGroup() {
        return group;
    }
}
