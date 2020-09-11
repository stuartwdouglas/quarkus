package io.quarkus.rest.test.resource.param.resource;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.junit.Assert;

import io.quarkus.rest.test.resource.param.HeaderParamsAsPrimitivesTest;

@Path("/array/default/null")
public class HeaderParamsAsPrimitivesResourceArrayDefaultNull {
    @GET
    @Produces("application/boolean")
    public String doGetBoolean(@HeaderParam("boolean") boolean[] v) {
        Assert.assertEquals(HeaderParamsAsPrimitivesTest.ERROR_MESSAGE, 0, v.length);
        return "content";
    }

    @GET
    @Produces("application/short")
    public String doGetShort(@HeaderParam("short") short[] v) {
        Assert.assertEquals(HeaderParamsAsPrimitivesTest.ERROR_MESSAGE, 0, v.length);
        return "content";
    }
}
