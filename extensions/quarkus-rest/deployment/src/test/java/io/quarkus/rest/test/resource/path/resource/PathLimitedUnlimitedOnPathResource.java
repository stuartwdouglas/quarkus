package io.quarkus.rest.test.resource.path.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/unlimited{param:.*}")
public class PathLimitedUnlimitedOnPathResource {
   @GET
   public String hello() {
      return "hello world";
   }
}
