package io.quarkus.rest.test.client;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestClientBuilder;
import javax.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import io.quarkus.rest.test.client.resource.TimeoutResource;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.Assert;
import io.quarkus.rest.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.rest.test.simple.TestUtil;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpSince RESTEasy 3.0.16
 */
public class TimeoutTest extends ClientTestBase{
   @Path("/timeout")
   public interface TimeoutResourceInterface {
      @GET
      @Produces("text/plain")
      String get(@QueryParam("sleep") int sleep) throws Exception;
   }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(TimeoutTest.class);
      war.addClass(ClientTestBase.class);
      return TestUtil.finishContainerPrepare(war, null, TimeoutResource.class);
   }});

   /**
    * @tpTestDetails Create client with custom SocketTimeout setting. Client sends GET request for the resource which
    * calls sleep() for the specified amount of time.
    * @tpPassCrit The request gets timeouted
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testTimeout() throws Exception {
      QuarkusRestClient clientengine = ((QuarkusRestClientBuilder)ClientBuilder.newBuilder()).readTimeout(2, TimeUnit.SECONDS).build();
      ClientHttpEngine engine = clientengine.httpEngine();
      Assert.assertNotNull("Client engine is was not created", engine);

      QuarkusRestClient client = ((QuarkusRestClientBuilder)ClientBuilder.newBuilder()).httpEngine(engine).build();
      ResteasyWebTarget target = client.target(generateURL("/timeout"));
      try {
         target.queryParam("sleep", "5").request().get();
         Assert.fail("The request didn't timeout as expected");
      } catch (ProcessingException e) {
         Assert.assertEquals("Expected SocketTimeoutException", e.getCause().getClass(), SocketTimeoutException.class);
      }

      TimeoutResourceInterface proxy = client.target(generateURL("")).proxy(TimeoutResourceInterface.class);
      try {
         proxy.get(5);
         Assert.fail("The request didn't timeout as expected when using client proxy");
      } catch (ProcessingException e) {
         Assert.assertEquals("Expected SocketTimeoutException", e.getCause().getClass(), SocketTimeoutException.class);
      }
      clientengine.close();
   }
}
