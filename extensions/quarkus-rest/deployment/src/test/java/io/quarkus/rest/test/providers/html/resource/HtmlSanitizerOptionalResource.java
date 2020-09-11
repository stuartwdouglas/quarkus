package io.quarkus.rest.test.providers.html.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import javax.ws.rs.core.Response.Status;

import io.quarkus.rest.test.providers.html.HtmlSanitizerOptionalTest;

@Path("")
public class HtmlSanitizerOptionalResource {

    @Path("test")
    @GET
    @Produces("text/html")
    public Response test() {
        return Response.status(Status.BAD_REQUEST).entity(HtmlSanitizerOptionalTest.input).build();
    }
}
