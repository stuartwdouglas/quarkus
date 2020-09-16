package io.quarkus.rest.test.form.resource;

import javax.ws.rs.FormParam;

import org.jboss.resteasy.annotations.Form;

public class NestedCollectionsFormTelephoneNumber {
    @Form(prefix = "country")
    public NestedCollectionsFormCountry country;
    @FormParam("number")
    public String number;
}
