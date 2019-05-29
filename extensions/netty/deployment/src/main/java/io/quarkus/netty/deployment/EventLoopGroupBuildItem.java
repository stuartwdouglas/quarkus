package io.quarkus.netty.deployment;

import java.util.function.Supplier;

import io.netty.channel.EventLoopGroup;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The main Netty {@link EventLoopGroup}. All Netty based frameworks should use this to
 * share resources
 */
public final class EventLoopGroupBuildItem extends SimpleBuildItem {

    private final Supplier<EventLoopGroup> group;

    public EventLoopGroupBuildItem(Supplier<EventLoopGroup> group) {
        this.group = group;
    }

    public Supplier<EventLoopGroup> getGroup() {
        return group;
    }
}
