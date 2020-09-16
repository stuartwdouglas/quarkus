package io.quarkus.rest.test.resource.param.resource;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import io.quarkus.rest.test.Assert;
import io.quarkus.rest.test.resource.param.QueryParamAsPrimitiveTest;

@Path("/wrappers/default")
public class QueryParamAsPrimitiveResourceWrappersDefault {
    @GET
    @Produces("application/boolean")
    public String doGet(@QueryParam("boolean") @DefaultValue("true") Boolean v) {
        Assert.assertEquals(QueryParamAsPrimitiveTest.ERROR_MESSAGE, true, v.booleanValue());
        return "content";
    }

    @GET
    @Produces("application/byte")
    public String doGet(@QueryParam("byte") @DefaultValue("127") Byte v) {
        Assert.assertTrue((byte) 127 == v.byteValue());
        return "content";
    }

    @GET
    @Produces("application/short")
    public String doGet(@QueryParam("short") @DefaultValue("32767") Short v) {
        Assert.assertTrue((short) 32767 == v.shortValue());
        return "content";
    }

    @GET
    @Produces("application/int")
    public String doGet(@QueryParam("int") @DefaultValue("2147483647") Integer v) {
        Assert.assertEquals(QueryParamAsPrimitiveTest.ERROR_MESSAGE, 2147483647, v.intValue());
        return "content";
    }

    @GET
    @Produces("application/long")
    public String doGet(@QueryParam("long") @DefaultValue("9223372036854775807") Long v) {
        Assert.assertEquals(QueryParamAsPrimitiveTest.ERROR_MESSAGE, 9223372036854775807L, v.longValue());
        return "content";
    }

    @GET
    @Produces("application/float")
    public String doGet(@QueryParam("float") @DefaultValue("3.14159265") Float v) {
        Assert.assertEquals(QueryParamAsPrimitiveTest.ERROR_MESSAGE, 3.14159265f, v.floatValue(), 0.0f);
        return "content";
    }

    @GET
    @Produces("application/double")
    public String doGet(@QueryParam("double") @DefaultValue("3.14159265358979") Double v) {
        Assert.assertEquals(QueryParamAsPrimitiveTest.ERROR_MESSAGE, 3.14159265358979d, v.doubleValue(), 0.0);
        return "content";
    }

    @GET
    @Produces("application/char")
    public String doGet(@QueryParam("char") @DefaultValue("a") Character v) {
        Assert.assertEquals(QueryParamAsPrimitiveTest.ERROR_MESSAGE, 'a', v.charValue());
        return "content";
    }
}
