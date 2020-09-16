package io.quarkus.rest.test.resource.param.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.quarkus.rest.test.Assert;
import io.quarkus.rest.test.resource.param.UriParamAsPrimitiveTest;

@Path("/short/wrapper/{arg}")
public class UriParamAsPrimitiveResourceUriShortWrapper {
    @GET
    public String doGet(@PathParam("arg") Short v) {
        Assert.assertTrue(UriParamAsPrimitiveTest.ERROR_CODE, 32767 == v.shortValue());
        return "content";
    }
}
