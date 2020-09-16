package io.quarkus.rest.test.core.encoding.resource;

import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.junit.jupiter.api.Assertions;

import io.quarkus.rest.test.core.encoding.EncodedParamsTest;

@Path("/encodedParam")
public class EncodedParamsComplexResource {
    @GET
    public String get(@QueryParam("hello world") int num, @QueryParam("stuff") @Encoded String stuff,
            @QueryParam("stuff") String unStuff) {
        Assertions.assertEquals(5, num, EncodedParamsTest.ERROR_MESSAGE);
        Assertions.assertEquals("hello%20world", stuff, EncodedParamsTest.ERROR_MESSAGE);
        Assertions.assertEquals("hello world", unStuff, EncodedParamsTest.ERROR_MESSAGE);
        return "HELLO";
    }

    @GET
    @Path("/{param}")
    public String goodbye(@PathParam("param") @Encoded String stuff) {
        Assertions.assertEquals("hello%20world", stuff, EncodedParamsTest.ERROR_MESSAGE);
        return "GOODBYE";
    }
}
