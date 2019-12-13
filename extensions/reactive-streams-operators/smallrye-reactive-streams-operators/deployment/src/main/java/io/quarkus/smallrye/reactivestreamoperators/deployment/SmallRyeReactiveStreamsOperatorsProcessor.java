package io.quarkus.smallrye.reactivestreamoperators.deployment;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreamsFactory;
import org.eclipse.microprofile.reactive.streams.operators.core.ReactiveStreamsFactoryImpl;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.smallrye.reactivestreamoperators.runtime.SmallRyeReactiveStreamsOperatorsRecorder;
import io.smallrye.reactive.streams.Engine;

public class SmallRyeReactiveStreamsOperatorsProcessor {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void build(BuildProducer<ServiceProviderBuildItem> serviceProvider,
            BuildProducer<FeatureBuildItem> feature,
            SmallRyeReactiveStreamsOperatorsRecorder recorder) {
        recorder.fixClassLoading();
        feature.produce(new FeatureBuildItem(FeatureBuildItem.SMALLRYE_REACTIVE_STREAMS_OPERATORS));
        serviceProvider.produce(new ServiceProviderBuildItem(ReactiveStreamsEngine.class.getName(), Engine.class.getName()));
        serviceProvider.produce(new ServiceProviderBuildItem(ReactiveStreamsFactory.class.getName(),
                ReactiveStreamsFactoryImpl.class.getName()));
    }

}
