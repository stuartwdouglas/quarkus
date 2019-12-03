package io.quarkus.bootstrap.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A representation of the Quarkus dependency model for a given application.
 *
 * @author Alexey Loubyansky
 */
public class AppModel {

    private final AppArtifact appArtifact;
    /**
     * The deployment dependencies, less the runtime parts. This will likely go away
     */
    private final List<AppDependency> deploymentDeps;
    /**
     * The deployment dependencies, including all transitive dependencies. This is used to build an isolated class
     * loader to run the augmentation
     */
    private final List<AppDependency> fullDeploymentDeps;

    /**
     * The runtime dependencies of the application, including the runtime parts of all extensions.
     */
    private final List<AppDependency> runtimeDeps;

    /**
     * A list of all dependencies, generated from deploymentDeps + runtimeDeps. This will likely go away.
     */
    private List<AppDependency> allDeps;

    public AppModel(AppArtifact appArtifact, List<AppDependency> runtimeDeps, List<AppDependency> deploymentDeps,
                    List<AppDependency> fullDeploymentDeps) {
        this.appArtifact = appArtifact;
        this.runtimeDeps = runtimeDeps;
        this.deploymentDeps = deploymentDeps;
        this.fullDeploymentDeps = fullDeploymentDeps;
    }

    @Deprecated
    public List<AppDependency> getAllDependencies() {
        if (allDeps == null) {
            allDeps = new ArrayList<>(runtimeDeps.size() + deploymentDeps.size());
            allDeps.addAll(runtimeDeps);
            allDeps.addAll(deploymentDeps);
        }
        return allDeps;
    }

    public AppArtifact getAppArtifact() {
        return appArtifact;
    }

    /**
     * Dependencies that the user has added that have nothing to do with Quarkus (3rd party libs, additional modules etc)
     */
    public List<AppDependency> getUserDependencies() {
        return runtimeDeps;
    }

    /**
     * Dependencies of the -deployment artifacts from the quarkus extensions, and all their transitive dependencies.
     *
     */
    @Deprecated
    public List<AppDependency> getDeploymentDependencies() {
        return deploymentDeps;
    }

    public List<AppDependency> getFullDeploymentDeps() {
        return fullDeploymentDeps;
    }
}
