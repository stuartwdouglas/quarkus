package io.quarkus.vertx.http.deployment;

import java.net.URI;
import java.util.function.Function;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.util.UriNormalizationUtil;
import io.quarkus.vertx.http.runtime.HandlerType;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public final class NonApplicationRootPathBuildItem extends SimpleBuildItem {
    /**
     * Normalized of quarkus.http.root-path.
     * Must end in a slash
     */
    final URI httpRootPath;

    /**
     * Normalized from quarkus.http.non-application-root-path
     */
    final URI nonApplicationRootPath;

    /**
     * Non-Application root path is distinct from HTTP root path.
     */
    final boolean dedicatedRouterRequired;

    final boolean attachedToMainRouter;

    public NonApplicationRootPathBuildItem(String httpRootPath, String nonApplicationRootPath) {
        // Presume value always starts with a slash and is normalized
        this.httpRootPath = UriNormalizationUtil.toURI(httpRootPath, true);

        this.nonApplicationRootPath = UriNormalizationUtil.normalizeWithBase(this.httpRootPath, nonApplicationRootPath,
                true);
        System.out.println(httpRootPath + " -> " + this.httpRootPath.getPath());
        System.out.println(nonApplicationRootPath + " -> " + this.nonApplicationRootPath.getPath());
        this.dedicatedRouterRequired = !this.nonApplicationRootPath.getPath().equals(this.httpRootPath.getPath());

        // Is the non-application root path underneath the http root path. Do we add non-application root to main router or not.
        this.attachedToMainRouter = this.nonApplicationRootPath.getPath().startsWith(this.httpRootPath.getPath());
    }

    /**
     * Is a dedicated router required for non-application endpoints.
     *
     * @return boolean
     */
    public boolean isDedicatedRouterRequired() {
        return dedicatedRouterRequired;
    }

    public boolean isAttachedToMainRouter() {
        return attachedToMainRouter;
    }

    /**
     * //TODO
     * Will return {@code null} if not nested, an empty string if identical,
     * or the nested portion of the path.
     *
     * @return String
     */
    String getVertxRouterPath() {
        if (attachedToMainRouter) {
            return "/" + relativize(httpRootPath.getPath(), nonApplicationRootPath.getPath());
        } else {
            return getNonApplicationRootPath();
        }
    }

    String relativize(String rootPath, String leafPath) {
        if (leafPath.startsWith(rootPath)) {
            return leafPath.substring(rootPath.length());
        }

        return null;
    }

    public String getNormalizedHttpRootPath() {
        return httpRootPath.getPath();
    }

    /**
     * Return normalized root path configured from {@literal quarkus.http.root-path}
     * and {quarkus.http.non-application-root-path}.
     * This path will always end in a slash.
     * <p>
     * Use {@link #resolvePath(String)} if you need to construct a URI for
     * a non-application endpoint.
     *
     * @return Normalized non-application root path ending with a slash
     * @see #resolvePath(String)
     */
    public String getNonApplicationRootPath() {
        return nonApplicationRootPath.getPath();
    }

    /**
     * Resolve path into an absolute path.
     * If path is relative, it will be resolved against `quarkus.http.non-application-root-path`.
     * An absolute path will be normalized and returned.
     * <p>
     * Given {@literal quarkus.http.root-path=/} and
     * {@literal quarkus.http.non-application-root-path="q"}
     * <ul>
     * <li>{@code resolvePath("foo")} will return {@literal /q/foo}</li>
     * <li>{@code resolvePath("/foo")} will return {@literal /foo}</li>
     * </ul>
     * <p>
     * Given {@literal quarkus.http.root-path=/} and
     * {@literal quarkus.http.non-application-root-path="/q"}
     * <ul>
     * <li>{@code resolvePath("foo")} will return {@literal /q/foo}</li>
     * <li>{@code resolvePath("/foo")} will return {@literal /foo}</li>
     * </ul>
     * Given {@literal quarkus.http.root-path=/app} and
     * {@literal quarkus.http.non-application-root-path="q"}
     * <ul>
     * <li>{@code resolvePath("foo")} will return {@literal /app/q/foo}</li>
     * <li>{@code resolvePath("/foo")} will return {@literal /foo}</li>
     * </ul>
     * Given {@literal quarkus.http.root-path=/app} and
     * {@literal quarkus.http.non-application-root-path="/q"}
     * <ul>
     * <li>{@code resolvePath("foo")} will return {@literal /q/foo}</li>
     * <li>{@code resolvePath("/foo")} will return {@literal /foo}</li>
     * </ul>
     * <p>
     * The returned path will not end with a slash.
     *
     * @param path Path to be resolved to an absolute path.
     * @return An absolute path not ending with a slash
     * @see UriNormalizationUtil#normalizeWithBase(URI, String, boolean)
     * @throws IllegalArgumentException if path is null or empty
     */
    public String resolvePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Specified path can not be empty");
        }
        return UriNormalizationUtil.normalizeWithBase(nonApplicationRootPath, path, false).getPath();
    }

    public Builder routeBuilder() {
        return new Builder(this);
    }

    /**
     * Per non-application endpoint instance.
     */
    public static class Builder extends RouteBuildItem.Builder {
        private final NonApplicationRootPathBuildItem buildItem;
        private boolean requiresLegacyRedirect = false;
        private boolean isFrameworkRoute = true;
        private String path;

        Builder(NonApplicationRootPathBuildItem buildItem) {
            this.buildItem = buildItem;
        }

        @Override
        public Builder routeFunction(Function<Router, Route> routeFunction) {
            //TODO See if we can do this without them needing to use the non app path in their processor
            super.routeFunction(routeFunction);
            return this;
        }

        @Override
        public Builder route(String route) {
            String temp = route;
            route = buildItem.resolvePath(route);

            this.isFrameworkRoute = buildItem.dedicatedRouterRequired
                    && route.startsWith(buildItem.getNonApplicationRootPath());

            if (isFrameworkRoute) {
                // relative non-application root
                this.path = "/" + buildItem.relativize(buildItem.getNonApplicationRootPath(), route);
            } else if (route.startsWith(buildItem.httpRootPath.getPath())) {
                // relative to http root
                this.path = "/" + buildItem.relativize(buildItem.httpRootPath.getPath(), route);
            } else if (route.startsWith("/")) {
                // absolute path
                this.path = route;
            }
            System.out.println(temp + " && " + isFrameworkRoute + " = " + this.path);
            System.out.println(buildItem.httpRootPath.getPath());
            System.out.println(buildItem.getNonApplicationRootPath());
            super.route(this.path);
            return this;
        }

        /**
         * @deprecated This will be removed in Quarkus 2.0, don't use unless you have to.
         */
        @Deprecated
        public Builder requiresLegacyRedirect() {
            this.requiresLegacyRedirect = true;
            return this;
        }

        public Builder handler(Handler<RoutingContext> handler) {
            super.handler(handler);
            return this;
        }

        public Builder handlerType(HandlerType handlerType) {
            super.handlerType(handlerType);
            return this;
        }

        public Builder blockingRoute() {
            super.blockingRoute();
            return this;
        }

        public Builder failureRoute() {
            super.failureRoute();
            return this;
        }

        public RouteBuildItem build() {
            // Is this part of non-application route or not

            return new RouteBuildItem(this, isFrameworkRoute, requiresLegacyRedirect);
        }
    }
}
