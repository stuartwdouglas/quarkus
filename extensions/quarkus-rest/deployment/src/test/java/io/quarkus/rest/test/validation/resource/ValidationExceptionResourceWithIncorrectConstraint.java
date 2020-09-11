package io.quarkus.rest.test.validation.resource;

import javax.validation.ConstraintDefinitionException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

@Path("/")
@ValidationExceptionIncorrectConstraint
public class ValidationExceptionResourceWithIncorrectConstraint {

   @Provider
   public static class ConstraintDefinitionExceptionMapper extends ValidationExceptionMapper<ConstraintDefinitionException> {
   }

   @POST
   public void test() {
   }
}
