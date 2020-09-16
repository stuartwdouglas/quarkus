package io.quarkus.rest.test.tracing;

import javax.ws.rs.GET;

public class Foo {
    @GET
    public String get() {
        return "{|FOO|}";
    }
}
