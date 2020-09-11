package io.quarkus.rest.test.client.proxy.resource;

import java.net.URI;

import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

public class ProxyJaxbResourceMessageResource implements ProxyJaxbResourcePostMessageIntf {

    private static Logger logger = Logger.getLogger(ProxyJaxbResourceMessageResource.class.getName());

    @Override
    public Response saveMessage(ProxyJaxbResourcePostMessage msg) {
        logger.info("saveMessage");
        return Response.created(URI.create("/foo/bar")).build();
    }
}
