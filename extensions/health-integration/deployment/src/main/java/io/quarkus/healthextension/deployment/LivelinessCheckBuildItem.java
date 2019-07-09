package io.quarkus.healthextension.deployment;

import java.util.function.Supplier;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.healthextension.HealthCheckResult;

public final class LivelinessCheckBuildItem extends MultiBuildItem {

    private final String name;
    /**
     * The supplier that provides the health check response. This is passed
     * to a recorder so must be either bytecode serializable or an object returned
     * from a recorder.
     */
    private final Supplier<HealthCheckResult> healthCheck;

    public LivelinessCheckBuildItem(String name, Supplier<HealthCheckResult> healthCheck) {
        this.name = name;
        this.healthCheck = healthCheck;
    }

    public String getName() {
        return name;
    }

    public Supplier<HealthCheckResult> getHealthCheck() {
        return healthCheck;
    }
}
