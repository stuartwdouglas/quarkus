package io.quarkus.runtime.jbang;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import io.quarkus.launcher.JBangRuntimeBuilder;

public class JBangIntegration {

    public static Map<String, byte[]> postBuild(Path classesDir, Path pomFile, List<Map.Entry<String, Path>> dependencies) {
        return JBangRuntimeBuilder.postBuild(classesDir, pomFile, dependencies);
    }

}
