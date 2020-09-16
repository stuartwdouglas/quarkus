package io.quarkus.rest.test.form.resource;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MultivaluedMap;

import io.quarkus.rest.runtime.util.MultivaluedMapImpl;

@Path("/myform")
public class FormResourceSecond {
    @GET
    @Path("/server")
    @Produces("application/x-www-form-urlencoded")
    public MultivaluedMap<String, String> retrieveServername() {

        MultivaluedMap<String, String> serverMap = new MultivaluedMapImpl<String, String>();
        serverMap.add("servername", "srv1");
        serverMap.add("servername", "srv2");

        return serverMap;
    }

    @POST
    public void post() {

    }
}
