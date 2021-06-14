package io.quarkus.resteasy.reactive.server.deployment;

import java.util.Optional;

import org.jboss.resteasy.reactive.server.model.FixedHandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.resteasy.reactive.server.runtime.observability.ObservabilityHandler;
import io.quarkus.runtime.metrics.MetricsFactory;

public class ObservabilityProcessor {

    @BuildStep
    void integrateObservability(Capabilities capabilities, Optional<MetricsCapabilityBuildItem> metricsCapability,
            BuildProducer<ServerResourceMethodCustomizerBuildItem> producer) {
        boolean integrationNeeded = (capabilities.isPresent(Capability.OPENTELEMETRY_TRACER) ||
                (metricsCapability.isPresent()
                        && metricsCapability.get().metricsSupported(MetricsFactory.MICROMETER)));
        if (!integrationNeeded) {
            return;
        }

        producer.produce(new ServerResourceMethodCustomizerBuildItem(
                new ServerResourceMethodCustomizerBuildItem.ServerResourceMethodCustomizer() {
                    @Override
                    public void customize(Context context) {
                        ObservabilityHandler observabilityHandler = new ObservabilityHandler();
                        observabilityHandler
                                .setTemplatePath(context.resourceClass().getPath() + context.resourceMethod().getPath());
                        context.resourceMethod().getHandlerChainCustomizers().add(new FixedHandlerChainCustomizer(
                                observabilityHandler, HandlerChainCustomizer.Phase.BEFORE_METHOD_INVOKE));
                    }
                }));
    }
}
