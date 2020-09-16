package io.quarkus.rest.test.resource.param.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.quarkus.rest.test.Assert;
import io.quarkus.rest.test.resource.param.ComplexPathParamTest;

@Path("/tricky")
public class ComplexPathParamTrickyResource {
    @GET
    @Path("{hello}")
    public String getHello(@PathParam("hello") int one) {
        Assert.assertEquals(ComplexPathParamTest.WRONG_REQUEST_ERROR_MESSAGE, one, 1);
        return "hello";
    }

    @GET
    @Path("{1},{2}")
    public String get2Groups(@PathParam("1") int one, @PathParam("2") int two) {
        Assert.assertEquals(ComplexPathParamTest.WRONG_REQUEST_ERROR_MESSAGE, 1, one);
        Assert.assertEquals(ComplexPathParamTest.WRONG_REQUEST_ERROR_MESSAGE, 2, two);
        return "2Groups";
    }

    @GET
    @Path("h{1}")
    public String getPrefixed(@PathParam("1") int one) {
        Assert.assertEquals(ComplexPathParamTest.WRONG_REQUEST_ERROR_MESSAGE, 1, one);
        return "prefixed";
    }
}
