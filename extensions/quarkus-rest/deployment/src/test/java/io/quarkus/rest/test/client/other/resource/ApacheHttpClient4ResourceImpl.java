package io.quarkus.rest.test.client.other.resource;

import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.spi.NoLogWebApplicationException;

public class ApacheHttpClient4ResourceImpl implements ApacheHttpClient4Resource {
    public String get() {
        return "hello world";
    }

    public String error() {
        throw new NoLogWebApplicationException(Status.NOT_FOUND);
    }

    public String getData(String data) {
        return "Here is your string:" + data;
    }
}
