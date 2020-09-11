package io.quarkus.rest.test.interceptor.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("test")
public class PriorityExecutionResource {
   @GET
   public String get() {
      return "test";
   }
}
