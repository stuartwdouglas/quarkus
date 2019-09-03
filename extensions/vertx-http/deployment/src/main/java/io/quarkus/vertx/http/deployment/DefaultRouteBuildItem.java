package io.quarkus.vertx.http.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * A build item that represents a handler for the default route
 */
public final class DefaultRouteBuildItem extends SimpleBuildItem {

    private final Handler<RoutingContext> handler;

    public DefaultRouteBuildItem(Handler<RoutingContext> handler) {
        this.handler = handler;
    }

    public Handler<RoutingContext> getHandler() {
        return handler;
    }
}
