package io.quarkus.rest.test.resource.param.resource;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.junit.Assert;

import io.quarkus.rest.test.resource.param.QueryParamAsPrimitiveTest;

@Path("/list/default/null")
public class QueryParamAsPrimitiveResourceListDefaultNull {
    @GET
    @Produces("application/boolean")
    public String doGetBoolean(@QueryParam("boolean") List<Boolean> v) {
        Assert.assertEquals(QueryParamAsPrimitiveTest.ERROR_MESSAGE, 0, v.size());
        return "content";
    }

    @GET
    @Produces("application/byte")
    public String doGetByte(@QueryParam("byte") List<Byte> v) {
        Assert.assertEquals(QueryParamAsPrimitiveTest.ERROR_MESSAGE, 0, v.size());
        return "content";
    }

    @GET
    @Produces("application/short")
    public String doGetShort(@QueryParam("short") List<Short> v) {
        Assert.assertEquals(QueryParamAsPrimitiveTest.ERROR_MESSAGE, 0, v.size());
        return "content";
    }

    @GET
    @Produces("application/int")
    public String doGetInteger(@QueryParam("int") List<Integer> v) {
        Assert.assertEquals(QueryParamAsPrimitiveTest.ERROR_MESSAGE, 0, v.size());
        return "content";
    }

    @GET
    @Produces("application/long")
    public String doGetLong(@QueryParam("long") List<Long> v) {
        Assert.assertEquals(QueryParamAsPrimitiveTest.ERROR_MESSAGE, 0, v.size());
        return "content";
    }

    @GET
    @Produces("application/float")
    public String doGetFloat(@QueryParam("float") List<Float> v) {
        Assert.assertEquals(QueryParamAsPrimitiveTest.ERROR_MESSAGE, 0, v.size());
        return "content";
    }

    @GET
    @Produces("application/double")
    public String doGetDouble(@QueryParam("double") List<Double> v) {
        Assert.assertEquals(QueryParamAsPrimitiveTest.ERROR_MESSAGE, 0, v.size());
        return "content";
    }

    @GET
    @Produces("application/char")
    public String doGetCharacter(@QueryParam("char") List<Character> v) {
        Assert.assertEquals(QueryParamAsPrimitiveTest.ERROR_MESSAGE, 0, v.size());
        return "content";
    }
}
