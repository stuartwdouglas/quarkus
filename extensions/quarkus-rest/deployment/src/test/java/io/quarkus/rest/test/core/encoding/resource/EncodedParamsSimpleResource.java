package io.quarkus.rest.test.core.encoding.resource;

import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.junit.jupiter.api.Assertions;

import io.quarkus.rest.test.core.encoding.EncodedParamsTest;

@Path("/encodedMethod")
public class EncodedParamsSimpleResource {
    @GET
    @Encoded
    public String get(@QueryParam("stuff") String stuff) {
        Assertions.assertEquals("hello%20world", stuff, EncodedParamsTest.ERROR_MESSAGE);
        return "HELLO";
    }

    @GET
    @Encoded
    @Path("/{param}")
    public String goodbye(@PathParam("param") String stuff) {
        Assertions.assertEquals("hello%20world", stuff, EncodedParamsTest.ERROR_MESSAGE);
        return "GOODBYE";
    }
}
