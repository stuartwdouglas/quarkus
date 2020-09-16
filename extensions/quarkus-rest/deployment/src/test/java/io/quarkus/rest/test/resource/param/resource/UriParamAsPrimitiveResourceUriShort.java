package io.quarkus.rest.test.resource.param.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.quarkus.rest.test.Assert;
import io.quarkus.rest.test.resource.param.UriParamAsPrimitiveTest;

@Path("/short/{arg}")
public class UriParamAsPrimitiveResourceUriShort {
    @GET
    public String doGet(@PathParam("arg") short v) {
        Assert.assertTrue(UriParamAsPrimitiveTest.ERROR_CODE, 32767 == v);
        return "content";
    }
}
