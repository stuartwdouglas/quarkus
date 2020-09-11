package io.quarkus.rest.test.exception.resource;

import javax.ws.rs.Path;

@Path("/test")
public class ResponseExceptionMapperRuntimeExceptionResource
        implements ResponseExceptionMapperRuntimeExceptionResourceInterface {

    public String get() {
        throw new RuntimeException("Test error");
    }
}
