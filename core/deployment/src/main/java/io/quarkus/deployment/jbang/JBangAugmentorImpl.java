package io.quarkus.deployment.jbang;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.BootstrapGradleException;
import io.quarkus.bootstrap.app.AdditionalDependency;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.model.WorkspaceModule;
import io.quarkus.bootstrap.util.QuarkusModelHelper;
import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.QuarkusAugmentor;
import io.quarkus.deployment.builditem.ApplicationClassNameBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.dev.IDEDevModeMain;
import io.quarkus.runtime.LaunchMode;

public class JBangAugmentorImpl implements BiConsumer<CuratedApplication, Map<String, Object>> {

    private static final Logger log = Logger.getLogger(IDEDevModeMain.class.getName());

    @Override
    public void accept(CuratedApplication curatedApplication, Map<String, Object> resultMap) {

        QuarkusClassLoader classLoader = curatedApplication.getAugmentClassLoader();

        QuarkusBootstrap quarkusBootstrap = curatedApplication.getQuarkusBootstrap();
        QuarkusAugmentor.Builder builder = QuarkusAugmentor.builder()
                .setRoot(quarkusBootstrap.getApplicationRoot())
                .setClassLoader(classLoader)
                .addFinal(ApplicationClassNameBuildItem.class)
                .setTargetDir(quarkusBootstrap.getTargetDirectory())
                .setDeploymentClassLoader(curatedApplication.createDeploymentClassLoader())
                .setBuildSystemProperties(quarkusBootstrap.getBuildSystemProperties())
                .setEffectiveModel(curatedApplication.getAppModel());
        if (quarkusBootstrap.getBaseName() != null) {
            builder.setBaseName(quarkusBootstrap.getBaseName());
        }

        builder.setLaunchMode(LaunchMode.NORMAL);
        builder.setRebuild(quarkusBootstrap.isRebuild());
        builder.setLiveReloadState(new LiveReloadBuildItem(false, Collections.emptySet(), new HashMap<>()));
        for (AdditionalDependency i : quarkusBootstrap.getAdditionalApplicationArchives()) {
            //this gets added to the class path either way
            //but we only need to add it to the additional app archives
            //if it is forced as an app archive
            if (i.isForceApplicationArchive()) {
                builder.addAdditionalApplicationArchive(i.getArchivePath());
            }
        }
        builder.excludeFromIndexing(quarkusBootstrap.getExcludeFromClassPath());
        builder.addFinal(GeneratedClassBuildItem.class);
        builder.addFinal(GeneratedResourceBuildItem.class);

        try {
            BuildResult buildResult = builder.build().run();
            Map<String, byte[]> result = new HashMap<>();
            for (GeneratedClassBuildItem i : buildResult.consumeMulti(GeneratedClassBuildItem.class)) {
                result.put(i.getName().replace(".", "/") + ".class", i.getClassData());
            }
            for (GeneratedResourceBuildItem i : buildResult.consumeMulti(GeneratedResourceBuildItem.class)) {
                result.put(i.getName(), i.getClassData());
            }
            resultMap.put("result", result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private DevModeContext.ModuleInfo toModule(WorkspaceModule module) throws BootstrapGradleException {
        AppArtifactKey key = new AppArtifactKey(module.getArtifactCoords().getGroupId(),
                module.getArtifactCoords().getArtifactId(), module.getArtifactCoords().getClassifier());

        Set<String> sourceDirectories = new HashSet<>();
        Set<String> sourceParents = new HashSet<>();
        for (File srcDir : module.getSourceSourceSet().getSourceDirectories()) {
            sourceDirectories.add(srcDir.getPath());
            sourceParents.add(srcDir.getParent());
        }

        return new DevModeContext.ModuleInfo(key,
                module.getArtifactCoords().getArtifactId(),
                module.getProjectRoot().getPath(),
                sourceDirectories,
                QuarkusModelHelper.getClassPath(module).toAbsolutePath().toString(),
                module.getSourceSourceSet().getResourceDirectory().toString(),
                module.getSourceSet().getResourceDirectory().getPath(),
                sourceParents,
                module.getBuildDir().toPath().resolve("generated-sources").toAbsolutePath().toString(),
                module.getBuildDir().toString());
    }

    private DevModeContext.ModuleInfo toModule(LocalProject project) {
        return new DevModeContext.ModuleInfo(project.getKey(), project.getArtifactId(),
                project.getDir().toAbsolutePath().toString(),
                Collections.singleton(project.getSourcesSourcesDir().toAbsolutePath().toString()),
                project.getClassesDir().toAbsolutePath().toString(),
                project.getResourcesSourcesDir().toAbsolutePath().toString(),
                project.getSourcesDir().toString(),
                project.getCodeGenOutputDir().toString(),
                project.getOutputDir().toString());
    }
}
