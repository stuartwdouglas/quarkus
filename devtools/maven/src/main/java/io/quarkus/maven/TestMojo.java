package io.quarkus.maven;

import java.util.function.Consumer;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import io.quarkus.deployment.dev.DevModeContext;

/**
 * The continous testing mojo
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class TestMojo extends DevMojo {
    @Override
    protected void modifyDevModeContext(MavenDevModeLauncher.Builder builder) {
        builder.entryPointCustomizer(new Consumer<DevModeContext>() {
            @Override
            public void accept(DevModeContext devModeContext) {

            }
        });
    }
}
