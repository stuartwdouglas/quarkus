package io.quarkus.rest.test.resource.param.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.quarkus.rest.test.Assert;
import io.quarkus.rest.test.resource.param.UriParamAsPrimitiveTest;

@Path("/double/wrapper/{arg}")
public class UriParamAsPrimitiveResourceUriDoubleWrapper {
    @GET
    public String doGet(@PathParam("arg") Double v) {
        Assert.assertEquals(UriParamAsPrimitiveTest.ERROR_CODE, 3.14159265358979d, v.doubleValue(), 0.0);
        return "content";
    }
}
