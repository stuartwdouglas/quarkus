package io.quarkus.rest.test.resource.path.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.quarkus.rest.test.Assert;

@Path("/digits")
public class PathParamDigits {
    @Path("{id:\\d+}")
    @GET
    public String get(@PathParam("id") int id) {
        Assert.assertEquals("Wrong request parameter", 5150, id);
        return Integer.toString(id);
    }
}
