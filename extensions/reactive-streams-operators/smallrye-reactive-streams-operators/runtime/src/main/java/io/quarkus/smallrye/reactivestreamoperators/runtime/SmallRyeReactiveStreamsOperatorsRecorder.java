package io.quarkus.smallrye.reactivestreamoperators.runtime;

import org.eclipse.microprofile.reactive.streams.operators.core.ReactiveStreamsEngineResolver;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsFactoryResolver;

import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.util.BrokenMpDelegationClassLoader;

@Recorder
public class SmallRyeReactiveStreamsOperatorsRecorder {

    //see https://github.com/eclipse/microprofile-reactive-streams-operators/pull/130
    public void fixClassLoading() {
        BrokenMpDelegationClassLoader.setupBrokenClWorkaround();
        try {
            ReactiveStreamsFactoryResolver.instance();
            ReactiveStreamsEngineResolver.instance();
        } finally {
            BrokenMpDelegationClassLoader.teardownBrokenClWorkaround();
        }
    }
}
