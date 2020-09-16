package io.quarkus.rest.test.resource.path.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("main/{key}")
public class WildcardMatchingSubResource {
    @GET
    public String subresource() {
        return this.getClass().getSimpleName();
    }
}
