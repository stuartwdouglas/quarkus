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

    public String getNonApplicationRootPath() {
        return nonApplicationRootPath.getPath();
    }

    public boolean isSeparateRouterRequired() {
        return separateRouterRequired;
    }

    /**
     * Resolve path into an absolute path.
     * If path is relative, it will be resolved against `quarkus.http.non-application-root-path`.
     * An absolute path will be normalized and returned.
     *
     * @param path Path to be resolved to an absolute path.
     * @return An absolute path
     */
    public String resolvePath(String path) {
        return UriNormalizationUtil.normalizeWithBase(nonApplicationRootPath, path, false).getPath();
    }
}
