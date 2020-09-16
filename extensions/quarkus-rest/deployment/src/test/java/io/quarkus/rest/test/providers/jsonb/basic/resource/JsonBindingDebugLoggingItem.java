package io.quarkus.rest.test.providers.jsonb.basic.resource;

public class JsonBindingDebugLoggingItem {
    Integer a;

    public Integer getA() {
        return a;
    }

    public JsonBindingDebugLoggingItem setA(Integer a) {
        this.a = a;
        return this;
    }
}
