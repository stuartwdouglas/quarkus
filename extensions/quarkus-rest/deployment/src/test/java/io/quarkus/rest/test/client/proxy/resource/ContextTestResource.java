package io.quarkus.rest.test.client.proxy.resource;

import javax.ws.rs.core.UriInfo;

import io.quarkus.rest.test.Assert;
import io.quarkus.rest.test.client.proxy.ContextTest;

public class ContextTestResource implements ContextTest.ResourceInterface {

    public String echo(UriInfo info) {
        Assert.assertNotNull("UriInfo was not injected into methods call", info);
        return "content";
    }
}
