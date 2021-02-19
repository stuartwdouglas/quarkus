package io.quarkus.vertx.http.deployment;

import java.net.URI;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.util.UriNormalizationUtil;

public final class NonApplicationRootPathBuildItem extends SimpleBuildItem {
    /**
     * Normalized of quarkus.http.root-path.
     * Must end in a slash
     */
    protected final URI httpRootPath;

    /**
     * Normalized from quarkus.http.non-application-root-path
     */
    private final URI nonApplicationRootPath;

    private final boolean separateRouterRequired;

    public NonApplicationRootPathBuildItem(String httpRootPath, String nonApplicationRootPath) {
        // Presume value always starts with a slash and is normalized
        this.httpRootPath = UriNormalizationUtil.toURI(httpRootPath, true);

        this.nonApplicationRootPath = UriNormalizationUtil.normalizeWithBase(this.httpRootPath, nonApplicationRootPath,
                true);

        this.separateRouterRequired = !nonApplicationRootPath.equals(httpRootPath);
    }

    public boolean isSeparateRouterRequired() {
        return separateRouterRequired;
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
}
