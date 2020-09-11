package io.quarkus.rest.test.microprofile.restclient.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("todos")
@RegisterRestClient(baseUri = "https://jsonplaceholder.typicode.com")
public interface RestClientProxyRedeployRemoteService {

    @GET
    @Path("1")
    String get();
}
