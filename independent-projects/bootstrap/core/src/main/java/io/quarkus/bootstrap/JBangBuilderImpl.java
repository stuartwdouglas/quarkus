package io.quarkus.bootstrap;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JBangBuilderImpl {
    public static Map<String, byte[]> postBuild(Path appClasses, Path pomFile, List<Map.Entry<String, Path>> dependencies) {

        try {
            AppArtifact appArtifact = new AppArtifact("dev.jbang.user", "quarkus", null, "jar", "999-SNAPSHOT");
            appArtifact.setPath(appClasses);
            final QuarkusBootstrap.Builder builder = QuarkusBootstrap.builder()
                    .setBaseClassLoader(JBangBuilderImpl.class.getClassLoader())
                    .setProjectRoot(pomFile.getParent())
                    .setForcedDependencies(dependencies.stream().map(s -> {
                        String[] parts = s.getKey().split(":");
                        AppArtifact artifact;
                        if (parts.length == 3) {
                            artifact = new AppArtifact(parts[0], parts[1], parts[2]);
                        } else if (parts.length == 4) {
                            artifact = new AppArtifact(parts[0], parts[1], null, parts[2], parts[3]);
                        } else if (parts.length == 5) {
                            artifact = new AppArtifact(parts[0], parts[1], parts[3], parts[2], parts[4]);
                        } else {
                            throw new RuntimeException("Invalid artifact " + s.getKey());
                        }
                        artifact.setPath(s.getValue());
                        return new AppDependency(artifact, "compile");
                    }).collect(Collectors.toList()))
                    .setAppArtifact(appArtifact)
                    .setIsolateDeployment(true)
                    .setMode(QuarkusBootstrap.Mode.PROD);

            CuratedApplication app = builder
                    .build().bootstrap();

            Map<String, Object> output = new HashMap<>();
            app.runInAugmentClassLoader("io.quarkus.deployment.jbang.JBangAugmentorImpl", output);
            return (Map<String, byte[]>) output.get("result");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
