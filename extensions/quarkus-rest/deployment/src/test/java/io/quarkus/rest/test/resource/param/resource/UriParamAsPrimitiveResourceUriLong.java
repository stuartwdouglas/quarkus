package io.quarkus.rest.test.resource.param.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.quarkus.rest.test.Assert;
import io.quarkus.rest.test.resource.param.UriParamAsPrimitiveTest;

@Path("/long/{arg}")
public class UriParamAsPrimitiveResourceUriLong {
    @GET
    public String doGet(@PathParam("arg") long v) {
        Assert.assertEquals(UriParamAsPrimitiveTest.ERROR_CODE, 9223372036854775807L, v);
        return "content";
    }
}
