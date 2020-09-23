package io.quarkus.rest.test.simple;

import javax.ws.rs.BeanParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import io.quarkus.rest.runtime.injection.QuarkusRestInjectionContext;

public class SimpleBeanParam {
    @QueryParam("query")
    String query;

    @QueryParam("query")
    private String privateQuery;

    @QueryParam("query")
    protected String protectedQuery;

    @QueryParam("query")
    public String publicQuery;

    @HeaderParam("header")
    String header;

    @Context
    UriInfo uriInfo;

    @BeanParam
    OtherBeanParam otherBeanParam;

    public void __quarkus_rest_inject2(QuarkusRestInjectionContext ctx) {
        query = ctx.getQueryParameter("query");
        privateQuery = ctx.getQueryParameter("query");
        protectedQuery = ctx.getQueryParameter("query");
        publicQuery = ctx.getQueryParameter("query");
        header = ctx.getHeader("header");
        otherBeanParam.__quarkus_rest_inject2(ctx);
    }
}
