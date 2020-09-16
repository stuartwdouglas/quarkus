package io.quarkus.rest.test.resource.param.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.junit.Assert;

import io.quarkus.rest.test.resource.param.UriParamAsPrimitiveTest;

@Path("/double/{arg}")
public class UriParamAsPrimitiveResourceUriDouble {
    @GET
    public String doGet(@PathParam("arg") double v) {
        Assert.assertEquals(UriParamAsPrimitiveTest.ERROR_CODE, 3.14159265358979d, v, 0.0);
        return "content";
    }
}
