package io.quarkus.deployment.dev;

import java.util.function.BooleanSupplier;

public class DevServicesEnabled implements BooleanSupplier {

    final DevservicesConfig config;

    public DevServicesEnabled(DevservicesConfig config) {
        this.config = config;
    }

    @Override
    public boolean getAsBoolean() {
        return config.enabled;
    }
}
