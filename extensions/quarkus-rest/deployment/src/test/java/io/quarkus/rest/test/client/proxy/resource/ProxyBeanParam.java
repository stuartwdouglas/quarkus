package io.quarkus.rest.test.client.proxy.resource;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.quarkus.rest.PathParam;
import io.quarkus.rest.QueryParam;

/**
 * Created by Marek Marusic <mmarusic@redhat.com> on 3/4/19.
 */
@Path("/a")
public interface ProxyBeanParam {

    @Path("a/{p1}/{p2}/{p3}")
    @GET
    String getAll(@BeanParam Params beanParam, @PathParam String p2, @QueryParam String queryParam);
}
