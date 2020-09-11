package io.quarkus.rest.test.form.resource;

import javax.ws.rs.FormParam;

public class ComplexFormAddress {
   @FormParam("street")
   public String street;
}
