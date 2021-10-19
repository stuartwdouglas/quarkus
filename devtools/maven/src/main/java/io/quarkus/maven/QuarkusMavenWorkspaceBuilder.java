package io.quarkus.maven;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.model.Build;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;

import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.workspace.DefaultProcessedSources;
import io.quarkus.bootstrap.workspace.DefaultWorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import io.quarkus.maven.dependency.GAV;
import io.quarkus.paths.PathList;

class QuarkusMavenWorkspaceBuilder {

    static void loadModules(MavenProject project, ApplicationModelBuilder modelBuilder) {
    }

    static WorkspaceModule toProjectModule(MavenProject project) {
        final Build build = project.getBuild();
        final DefaultWorkspaceModule module = new DefaultWorkspaceModule(getId(project), project.getBasedir(),
                new File(build.getDirectory()));

        final Path classesDir = Paths.get(build.getOutputDirectory());
        project.getCompileSourceRoots()
                .forEach(s -> module.addMainSources(new DefaultProcessedSources(Paths.get(s), classesDir)));
        final Path testClassesDir = Paths.get(build.getTestOutputDirectory());
        project.getTestCompileSourceRoots()
                .forEach(s -> module.addTestSources(new DefaultProcessedSources(Paths.get(s), testClassesDir)));

        for (Resource r : build.getResources()) {
            module.addMainResources(new DefaultProcessedSources(Paths.get(r.getDirectory()),
                    r.getTargetPath() == null ? classesDir : Paths.get(r.getTargetPath())));
        }

        for (Resource r : build.getTestResources()) {
            module.addTestResources(new DefaultProcessedSources(Paths.get(r.getDirectory()),
                    r.getTargetPath() == null ? testClassesDir : Paths.get(r.getTargetPath())));
        }

        module.setBuildFiles(PathList.of(project.getFile().toPath()));

        return module;
    }

    private static WorkspaceModuleId getId(MavenProject project) {
        return new GAV(project.getGroupId(), project.getArtifactId(), project.getVersion());
    }
}
