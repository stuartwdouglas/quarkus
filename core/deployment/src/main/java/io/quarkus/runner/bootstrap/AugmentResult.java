package io.quarkus.runner.bootstrap;

import java.util.List;

import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;

/**
 * The result of an augmentation that builds an application
 */
public class AugmentResult {
    private final List<ArtifactResultBuildItem> results;
    private final JarBuildItem jar;
    private final NativeImageBuildItem nativeResult;

    public AugmentResult(List<ArtifactResultBuildItem> results, JarBuildItem jar, NativeImageBuildItem nativeResult) {
        this.results = results;
        this.jar = jar;
        this.nativeResult = nativeResult;
    }

    public List<ArtifactResultBuildItem> getResults() {
        return results;
    }

    public JarBuildItem getJar() {
        return jar;
    }

    public NativeImageBuildItem getNativeResult() {
        return nativeResult;
    }
}
