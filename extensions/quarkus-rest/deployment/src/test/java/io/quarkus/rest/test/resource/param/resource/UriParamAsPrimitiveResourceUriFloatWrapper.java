package io.quarkus.rest.test.resource.param.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.quarkus.rest.test.Assert;
import io.quarkus.rest.test.resource.param.UriParamAsPrimitiveTest;

@Path("/float/wrapper/{arg}")
public class UriParamAsPrimitiveResourceUriFloatWrapper {
    @GET
    public String doGet(@PathParam("arg") Float v) {
        Assert.assertEquals(UriParamAsPrimitiveTest.ERROR_CODE, 3.14159265f, v.floatValue(), 0.0f);
        return "content";
    }
}
