package io.quarkus.rest.test.resource.param.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.quarkus.rest.test.Assert;
import io.quarkus.rest.test.resource.param.UriParamAsPrimitiveTest;

@Path("/byte/{arg}")
public class UriParamAsPrimitiveResourceUriByte {
    @GET
    public String doGet(@PathParam("arg") byte v) {
        Assert.assertTrue(UriParamAsPrimitiveTest.ERROR_CODE, 127 == v);
        return "content";
    }
}
