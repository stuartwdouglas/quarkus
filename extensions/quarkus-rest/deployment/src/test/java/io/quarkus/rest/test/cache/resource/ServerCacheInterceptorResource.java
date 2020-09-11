package io.quarkus.rest.test.cache.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.jboss.resteasy.annotations.cache.Cache;

@Path("/")
public class ServerCacheInterceptorResource {

    private static int count = 0;

    @GET
    @Cache(maxAge = 3600)
    @Path("public")
    public String getPublicResource() {
        return String.valueOf(++count);
    }

    @GET
    @Cache(isPrivate = true, maxAge = 3600)
    @Path("private")
    public String getPrivateResouce() {
        return String.valueOf(++count);
    }

    @GET
    @Cache(noStore = true, maxAge = 3600)
    @Path("no-store")
    public String getNoStore() {
        return String.valueOf(++count);
    }

}
