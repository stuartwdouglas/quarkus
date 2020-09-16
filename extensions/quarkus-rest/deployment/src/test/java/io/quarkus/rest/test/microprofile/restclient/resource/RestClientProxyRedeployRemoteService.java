package org.jboss.resteasy.test.microprofile.restclient.resource;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("todos")
@RegisterRestClient(baseUri="https://jsonplaceholder.typicode.com")
public interface RestClientProxyRedeployRemoteService {

    @GET
    @Path("1")
    String get();
}
