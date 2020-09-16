package io.quarkus.rest.test.client.proxy.resource;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import io.quarkus.rest.CookieParam;
import io.quarkus.rest.FormParam;
import io.quarkus.rest.HeaderParam;
import io.quarkus.rest.MatrixParam;
import io.quarkus.rest.PathParam;
import io.quarkus.rest.QueryParam;

/**
 * Created by Marek Marusic <mmarusic@redhat.com> on 1/16/19.
 */
@Path("/")
public interface ProxyParameterAnotations {
    @Path("QueryParam")
    @GET
    String executeQueryParam(@QueryParam String queryParam);

    @Path("HeaderParam")
    @GET
    String executeHeaderParam(@HeaderParam String headerParam);

    @Path("CookieParam")
    @GET
    String executeCookieParam(@CookieParam String cookieParam);

    @Path("PathParam/{pathParam}")
    @GET
    String executePathParam(@PathParam String pathParam);

    @Path("FormParam")
    @POST
    String executeFormParam(@FormParam String formParam);

    @Path("MatrixParam")
    @GET
    String executeMatrixParam(@MatrixParam String matrixParam);

    @Path("AllParams/{pathParam}")
    @POST
    String executeAllParams(@QueryParam String queryParam,
            @HeaderParam String headerParam,
            @CookieParam String cookieParam,
            @PathParam String pathParam,
            @FormParam String formParam,
            @MatrixParam String matrixParam);
}
