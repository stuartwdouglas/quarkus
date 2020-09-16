package io.quarkus.rest.test.resource.param.resource;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import io.quarkus.rest.CookieParam;
import io.quarkus.rest.FormParam;
import io.quarkus.rest.HeaderParam;
import io.quarkus.rest.MatrixParam;
import io.quarkus.rest.PathParam;
import io.quarkus.rest.QueryParam;

@Path("/proxy")
public interface RESTEasyParamBasicProxy {
    @POST
    @Path("a/{pathParam3}")
    Response post(
            @CookieParam String cookieParam3,
            @FormParam String formParam3,
            @HeaderParam String headerParam3,
            @MatrixParam String matrixParam3,
            @PathParam String pathParam3,
            @QueryParam String queryParam3);
}
