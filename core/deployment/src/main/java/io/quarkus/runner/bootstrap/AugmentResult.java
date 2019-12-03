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
    private final JarBuildItem jarResult;
    private final NativeImageBuildItem nativeResult;

    public AugmentResult(List<ArtifactResultBuildItem> results, JarBuildItem jarResult, NativeImageBuildItem nativeResult) {
        this.results = results;
        this.jarResult = jarResult;
        this.nativeResult = nativeResult;
    }

    public List<ArtifactResultBuildItem> getResults() {
        return results;
    }

    public JarBuildItem getJarResult() {
        return jarResult;
    }

    public NativeImageBuildItem getNativeResult() {
        return nativeResult;
    }
}
