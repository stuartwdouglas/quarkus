package io.quarkus.rest.test.resource.param.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.quarkus.rest.test.Assert;
import io.quarkus.rest.test.resource.param.UriParamAsPrimitiveTest;

@Path("/int/{arg}")
public class UriParamAsPrimitiveResourceUriInt {
    @GET
    public String doGet(@PathParam("arg") int v) {
        Assert.assertEquals(UriParamAsPrimitiveTest.ERROR_CODE, 2147483647, v);
        return "content";
    }
}
