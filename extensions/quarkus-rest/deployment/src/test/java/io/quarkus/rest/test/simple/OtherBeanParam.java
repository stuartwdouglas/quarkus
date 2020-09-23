package io.quarkus.rest.test.simple;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import io.quarkus.rest.runtime.injection.QuarkusRestInjectionContext;

public class OtherBeanParam {
    @QueryParam("query")
    String query;

    @HeaderParam("header")
    String header;

    @Context
    UriInfo uriInfo;

    public void __quarkus_rest_inject2(QuarkusRestInjectionContext ctx) {
        query = ctx.getQueryParameter("query");
        header = ctx.getHeader("header");
    }
}
