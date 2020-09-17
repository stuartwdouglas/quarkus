package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.HashMap;

import io.vertx.ext.web.RoutingContext;

public class MetricsContext extends HashMap<String, Object> {

    static final String HTTP_REQUEST_PATH = "HTTP_REQUEST_PATH";
    static final String HTTP_REQUEST_SAMPLE = "HTTP_REQUEST_SAMPLE";

    volatile RoutingContext routingContext;

    <T> T getFromRoutingContext(String key) {
        if (routingContext != null) {
            return routingContext.get(key);
        }
        return null;
    }

    <T> T getValue(String key) {
        Object o = this.get(key);
        return o == null ? null : (T) o;
    }
}
