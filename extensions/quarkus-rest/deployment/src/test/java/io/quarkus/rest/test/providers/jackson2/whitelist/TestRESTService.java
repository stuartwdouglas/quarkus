package io.quarkus.rest.test.providers.jackson2.whitelist;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import javax.ws.rs.core.Response.Status;

import io.quarkus.rest.test.providers.jackson2.whitelist.model.TestPolymorphicType;

/**
 * @author bmaxwell
 */
@Path("/test")
public class TestRESTService {
    @POST
    @Path("/post")
    @Consumes("application/json")
    public Response postTest(TestPolymorphicType test) {
        return Response.status(Status.CREATED).entity("Test success: " + test).build();
    }
}
