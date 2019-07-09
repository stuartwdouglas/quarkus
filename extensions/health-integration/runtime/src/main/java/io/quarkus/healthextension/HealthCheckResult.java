package io.quarkus.healthextension;

import java.util.Map;
import java.util.Optional;

public class HealthCheckResult {

    private final boolean up;
    private final Map<String, Object> data;

    public HealthCheckResult(boolean up, Map<String, Object> data) {
        this.up = up;
        this.data = data;
    }

    public boolean isUp() {
        return up;
    }

    public Optional<Map<String, Object>> getData() {
        return Optional.ofNullable(data);
    }
}
