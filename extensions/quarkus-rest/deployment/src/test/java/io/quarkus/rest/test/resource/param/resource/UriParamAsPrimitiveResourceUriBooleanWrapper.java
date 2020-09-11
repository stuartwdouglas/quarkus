package io.quarkus.rest.test.resource.param.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.junit.Assert;

import io.quarkus.rest.test.resource.param.UriParamAsPrimitiveTest;

@Path("/boolean/wrapper/{arg}")
public class UriParamAsPrimitiveResourceUriBooleanWrapper {
    @GET
    public String doGet(@PathParam("arg") Boolean v) {
        Assert.assertEquals(UriParamAsPrimitiveTest.ERROR_CODE, true, v.booleanValue());
        return "content";
    }
}
