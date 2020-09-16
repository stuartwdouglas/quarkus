package io.quarkus.rest.test.form.resource;

import javax.ws.rs.FormParam;

import org.jboss.resteasy.annotations.Form;

public class NestedCollectionsFormAddress {
    @FormParam("street")
    public String street;
    @FormParam("houseNumber")
    public String houseNumber;
    @Form(prefix = "country")
    public NestedCollectionsFormCountry country;
}
