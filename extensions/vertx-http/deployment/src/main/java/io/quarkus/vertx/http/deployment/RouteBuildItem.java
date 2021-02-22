package io.quarkus.vertx.http.deployment;

import java.util.function.Function;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.vertx.http.runtime.BasicRoute;
import io.quarkus.vertx.http.runtime.HandlerType;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public final class RouteBuildItem extends MultiBuildItem {

    public static Builder builder() {
        return new Builder();
    }

    private final Function<Router, Route> routeFunction;
    private final Handler<RoutingContext> handler;
    private final HandlerType type;
    private final boolean isFrameworkRoute;
    private final boolean requiresLegacyRedirect;

    /**
     * @deprecated Use the Builder instead.
     */
    @Deprecated
    public RouteBuildItem(Function<Router, Route> routeFunction, Handler<RoutingContext> handler, HandlerType type) {
        this.routeFunction = routeFunction;
        this.handler = handler;
        this.type = type;
        this.isFrameworkRoute = false;
        this.requiresLegacyRedirect = false;
    }

    /**
     * @deprecated Use the Builder instead.
     */
    @Deprecated
    public RouteBuildItem(Function<Router, Route> routeFunction, Handler<RoutingContext> handler) {
        this(routeFunction, handler, HandlerType.NORMAL);
    }

    /**
     * @deprecated Use the Builder instead.
     */
    @Deprecated
    public RouteBuildItem(String route, Handler<RoutingContext> handler, HandlerType type, boolean resume) {
        this(new BasicRoute(route), handler, type);
    }

    /**
     * @deprecated Use the Builder instead.
     */
    @Deprecated
    public RouteBuildItem(String route, Handler<RoutingContext> handler, HandlerType type) {
        this(new BasicRoute(route), handler, type);
    }

    /**
     * @deprecated Use the Builder instead.
     */
    @Deprecated
    public RouteBuildItem(String route, Handler<RoutingContext> handler, boolean resume) {
        this(new BasicRoute(route), handler, HandlerType.NORMAL);
    }

    /**
     * @deprecated Use the Builder instead.
     */
    @Deprecated
    public RouteBuildItem(String route, Handler<RoutingContext> handler) {
        this(new BasicRoute(route), handler);
    }

    RouteBuildItem(Builder builder, boolean isFrameworkRoute, boolean requiresLegacyRedirect) {
        this.routeFunction = builder.routeFunction;
        this.handler = builder.handler;
        this.type = builder.type;
        this.isFrameworkRoute = isFrameworkRoute;
        this.requiresLegacyRedirect = requiresLegacyRedirect;
    }

    public Handler<RoutingContext> getHandler() {
        return handler;
    }

    public HandlerType getType() {
        return type;
    }

    public Function<Router, Route> getRouteFunction() {
        return routeFunction;
    }

    public boolean isFrameworkRoute() {
        return isFrameworkRoute;
    }

    public boolean isRequiresLegacyRedirect() {
        return requiresLegacyRedirect;
    }

    /**
     * NonApplicationRootPathBuildItem.Builder extends this.
     * Please verify the extended builders behavior when changing this one.
     */
    public static class Builder {
        private Function<Router, Route> routeFunction;
        private Handler<RoutingContext> handler;
        private HandlerType type = HandlerType.NORMAL;

        public Builder routeFunction(Function<Router, Route> routeFunction) {
            this.routeFunction = routeFunction;
            return this;
        }

        public Builder route(String route) {
            this.routeFunction = new BasicRoute(route);
            return this;
        }

        public Builder handler(Handler<RoutingContext> handler) {
            this.handler = handler;
            return this;
        }

        public Builder handlerType(HandlerType handlerType) {
            this.type = handlerType;
            return this;
        }

        public Builder blockingRoute() {
            this.type = HandlerType.BLOCKING;
            return this;
        }

        public Builder failureRoute() {
            this.type = HandlerType.FAILURE;
            return this;
        }

        public RouteBuildItem build() {
            return new RouteBuildItem(this, false, false);
        }
    }
}
