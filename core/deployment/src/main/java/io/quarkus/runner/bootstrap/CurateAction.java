package io.quarkus.runner.bootstrap;

import io.quarkus.bootstrap.BootstrapAppModelFactory;
import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.model.AppModel;

public class CurateAction {

    private QuarkusBootstrap quarkusBootstrap;
    private boolean offline;
    private QuarkusClassPathResolver runtimeClassPath = new DefaultMavenClassPathResolver();

    CurateAction(QuarkusBootstrap quarkusBootstrap) {
        this.quarkusBootstrap = quarkusBootstrap;
    }

    public QuarkusClassPathResolver getRuntimeClassPath() {
        return runtimeClassPath;
    }

    public CurateAction setRuntimeClassPath(QuarkusClassPathResolver runtimeClassPath) {
        this.runtimeClassPath = runtimeClassPath;
        return this;
    }

    public boolean isOffline() {
        return offline;
    }

    public CurateAction setOffline(boolean offline) {
        this.offline = offline;
        return this;
    }

    public AugmentAction curate() {
        try {
            //this is super simple, all we want to do is resolve all our dependencies
            //once we have this it is up to augment to set up the class loader to actually use them
            BootstrapAppModelFactory appModelFactory = BootstrapAppModelFactory.newInstance()
                    .setAppClasses(quarkusBootstrap.getProjectRoot() != null ? quarkusBootstrap.getProjectRoot() : quarkusBootstrap.getApplicationRoot());
            AppModel model = appModelFactory
                    .resolveAppModel();
            return new AugmentAction(quarkusBootstrap, model, appModelFactory.getAppModelResolver());
        } catch (BootstrapException e) {
            throw new RuntimeException(e);
        }
    }

}
