package io.quarkus.rest.test.response.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
public class WriterMatchingResource {
   @GET
   @Path("bool")
   public Boolean responseOk() {
      return true;
   }
}
