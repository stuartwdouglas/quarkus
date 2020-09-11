package io.quarkus.rest.test.client.proxy.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.ClientBuilder;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import org.jboss.resteasy.client.jaxrs.internal.proxy.ProxyBuilderImpl;
import io.quarkus.rest.test.core.smoke.resource.ResourceWithInterfaceSimpleClient;

@Path("/cl")
public class ClassloaderResource
{

   @GET
   @Path("cl")
   @Produces("text/plain")
   public String test(@QueryParam("param") String targetBase) throws Exception
   {
      ClassLoader orig = Thread.currentThread().getContextClassLoader();
      //create the client...
      QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
      try
      {
         //replace the TCCL with the classloader from the resteasy-client module
         Thread.currentThread().setContextClassLoader(ProxyBuilderImpl.class.getClassLoader());
         //try building the proxy; the TCCL does not have visibility over the deployment classes
         ResourceWithInterfaceSimpleClient proxy = client.target(targetBase)
               .proxyBuilder(ResourceWithInterfaceSimpleClient.class).build();

         return proxy.getBasic();
      }
      finally
      {
         client.close();
         //restore original TCCL
         Thread.currentThread().setContextClassLoader(orig);
      }
   }

}
