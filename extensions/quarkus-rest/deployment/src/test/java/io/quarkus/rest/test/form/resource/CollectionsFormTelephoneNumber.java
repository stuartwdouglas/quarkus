package io.quarkus.rest.test.form.resource;

import javax.ws.rs.FormParam;

public class CollectionsFormTelephoneNumber {
   @FormParam("countryCode")
   public String countryCode;
   @FormParam("number")
   public String number;
}
