package io.quarkus.maven;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * The dev mojo, that connects to a remote host.
 */
@Mojo(name = "remote-dev", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.TEST)
public class RemoteDevMojo extends DevMojo {

    @Parameter(defaultValue = "${localDebug}")
    private String localDebug;

    @Parameter(defaultValue = "${localSuspend}")
    private String localSuspend;

    @Parameter(defaultValue = "${localDebugHost}")
    private String localDebugHost;

    @Parameter(defaultValue = "${localDebugPort}")
    private String localDebugPort;

    @Override
    protected void modifyDevModeContext(MavenDevModeLauncher.Builder builder) {
        //by default we don't start the local process in debug mode
        //as we are going to do HTTP upgrade trickery
        builder.debug(localDebug == null ? "false" : localDebug)
                .suspend(localSuspend)
                .debugHost(localDebugHost)
                .debugPort(localDebugPort);

        builder.remoteDev(true);
    }
}
