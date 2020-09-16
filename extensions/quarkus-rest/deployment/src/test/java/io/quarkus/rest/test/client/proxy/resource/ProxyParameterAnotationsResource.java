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
public class ProxyParameterAnotationsResource {
    @Path("QueryParam")
    @GET
    public String getQueryParam(@QueryParam("queryParam") String queryParam) {
        return "QueryParam = " + queryParam;
    }

    @Path("HeaderParam")
    @GET
    public String getHeaderParam(@HeaderParam("headerParam") String headerParam) {
        return "HeaderParam = " + headerParam;
    }

    @Path("CookieParam")
    @GET
    public String getCookieParam(@CookieParam("cookieParam") String cookieParam) {
        return "CookieParam = " + cookieParam;
    }

    @Path("PathParam/{pathParam}")
    @GET
    public String getPathParam(@PathParam("pathParam") String pathParam) {
        return "PathParam = " + pathParam;
    }

    @Path("FormParam")
    @POST
    public String getFormParam(@FormParam("formParam") String formParam) {
        return "FormParam = " + formParam;
    }

    @Path("MatrixParam")
    @GET
    public String getMatrixParam(@MatrixParam("matrixParam") String matrixParam) {
        return "MatrixParam = " + matrixParam;
    }

    @Path("AllParams/{pathParam}")
    @POST
    public String getAllParams(@QueryParam String queryParam,
            @HeaderParam String headerParam,
            @CookieParam String cookieParam,
            @PathParam String pathParam,
            @FormParam String formParam,
            @MatrixParam String matrixParam) {
        return queryParam + " " + headerParam + " " + cookieParam + " " + pathParam + " " + formParam + " " + matrixParam;
    }
}
