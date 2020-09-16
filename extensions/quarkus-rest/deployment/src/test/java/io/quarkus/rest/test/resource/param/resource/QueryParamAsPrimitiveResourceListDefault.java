package io.quarkus.rest.test.resource.param.resource;

import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import io.quarkus.rest.test.Assert;
import io.quarkus.rest.test.resource.param.QueryParamAsPrimitiveTest;

@Path("/list/default")
public class QueryParamAsPrimitiveResourceListDefault {
    @GET
    @Produces("application/boolean")
    public String doGetBoolean(@QueryParam("boolean") @DefaultValue("true") List<Boolean> v) {
        Assert.assertEquals(QueryParamAsPrimitiveTest.ERROR_MESSAGE, true, v.get(0).booleanValue());
        return "content";
    }

    @GET
    @Produces("application/byte")
    public String doGetByte(@QueryParam("byte") @DefaultValue("127") List<Byte> v) {
        Assert.assertTrue((byte) 127 == v.get(0).byteValue());
        return "content";
    }

    @GET
    @Produces("application/short")
    public String doGetShort(@QueryParam("short") @DefaultValue("32767") List<Short> v) {
        Assert.assertTrue((short) 32767 == v.get(0).shortValue());
        return "content";
    }

    @GET
    @Produces("application/int")
    public String doGetInteger(@QueryParam("int") @DefaultValue("2147483647") List<Integer> v) {
        Assert.assertEquals(QueryParamAsPrimitiveTest.ERROR_MESSAGE, 2147483647, v.get(0).intValue());
        return "content";
    }

    @GET
    @Produces("application/long")
    public String doGetLong(@QueryParam("long") @DefaultValue("9223372036854775807") List<Long> v) {
        Assert.assertEquals(QueryParamAsPrimitiveTest.ERROR_MESSAGE, 9223372036854775807L, v.get(0).longValue());
        return "content";
    }

    @GET
    @Produces("application/float")
    public String doGetFloat(@QueryParam("float") @DefaultValue("3.14159265") List<Float> v) {
        Assert.assertEquals(QueryParamAsPrimitiveTest.ERROR_MESSAGE, 3.14159265f, v.get(0).floatValue(), 0.0f);
        return "content";
    }

    @GET
    @Produces("application/double")
    public String doGetDouble(@QueryParam("double") @DefaultValue("3.14159265358979") List<Double> v) {
        Assert.assertEquals(QueryParamAsPrimitiveTest.ERROR_MESSAGE, 3.14159265358979d, v.get(0).doubleValue(), 0.0);
        return "content";
    }

    @GET
    @Produces("application/char")
    public String doGetCharacter(@QueryParam("char") @DefaultValue("a") List<Character> v) {
        Assert.assertEquals(QueryParamAsPrimitiveTest.ERROR_MESSAGE, 'a', v.get(0).charValue());
        return "content";
    }
}
