package io.quarkus.vertx.http.deployment;

import java.net.URI;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.util.UriNormalizationUtil;

public final class HttpRootPathBuildItem extends SimpleBuildItem {

    /**
     * Normalized from quarkus.http.root-path.
     * Will end in a slash
     */
    private final URI rootPath;

    public HttpRootPathBuildItem(String rootPath) {
        this.rootPath = UriNormalizationUtil.toURI(rootPath, true);
    }

    public String getRootPath() {
        return rootPath.getPath();
    }

    /**
     * Resolve path into an absolute path.
     * If path is relative, it will be resolved against `quarkus.http.root-path`.
     * An absolute path will be normalized and returned.
     *
     * @param path Path to be resolved to an absolute path.
     * @return An absolute path
     */
    public String resolvePath(String path) {
        return UriNormalizationUtil.normalizeWithBase(rootPath, path, false).getPath();
    }
}
