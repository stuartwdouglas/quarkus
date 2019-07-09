package io.quarkus.smallrye.health.runtime;

import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.spi.HealthCheckResponseProvider;

import io.quarkus.healthextension.HealthCheckResult;
import io.quarkus.runtime.annotations.Template;

@Template
public class SmallRyeHealthTemplate {

    public void registerHealthCheckResponseProvider(Class<? extends HealthCheckResponseProvider> providerClass) {
        try {
            HealthCheckResponse.setResponseProvider(providerClass.getConstructor().newInstance());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to instantiate service " + providerClass + " using the no-arg constructor.");
        }
    }

    public Supplier<Object> createHealthCheck(String name, Supplier<HealthCheckResult> resultSupplier) {
        return new Supplier<Object>() {
            @Override
            public Object get() {
                return new HealthCheck() {
                    @Override
                    public HealthCheckResponse call() {
                        HealthCheckResult result = resultSupplier.get();
                        HealthCheckResponseBuilder builder = HealthCheckResponse.builder();
                        builder.state(result.isUp());
                        builder.name(name);
                        if (result.getData().isPresent()) {
                            for (Map.Entry<String, Object> e : result.getData().get().entrySet()) {
                                builder.withData(e.getKey(), e.getValue().toString()); //TODO: could be better
                            }
                        }
                        return builder.build();
                    }
                };
            }
        };
    }

}
