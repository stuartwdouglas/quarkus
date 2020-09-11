package io.quarkus.rest.test.resource.param.resource;

import java.util.SortedSet;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.junit.Assert;

import io.quarkus.rest.test.resource.param.HeaderParamsAsPrimitivesTest;

@Path("/sortedset")
public class HeaderParamsAsPrimitivesResourceSortedSet implements HeaderParamsAsPrimitivesSortedSetProxy {
    @GET
    @Produces("application/boolean")
    public String doGetBoolean(@HeaderParam("header") SortedSet<String> v) {
        Assert.assertEquals(HeaderParamsAsPrimitivesTest.ERROR_MESSAGE, 2, v.size());
        Assert.assertTrue(HeaderParamsAsPrimitivesTest.ERROR_MESSAGE, v.contains("one"));
        Assert.assertTrue(HeaderParamsAsPrimitivesTest.ERROR_MESSAGE, v.contains("two"));
        return "content";
    }
}
