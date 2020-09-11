package io.quarkus.rest.test.client.proxy;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.client.proxy.resource.ClientResponseFailureResource;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.Assert;
import io.quarkus.rest.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.rest.test.simple.TestUtil;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class ClientResponseFailureTest {

   @Path("/test")
   public interface ClientResponseFailureResourceInterface {
      @GET
      @Produces("text/plain")
      String get();

      @GET
      @Path("error")
      @Produces("text/plain")
      String error();
   }

   static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(ClientResponseFailureTest.class);
      return TestUtil.finishContainerPrepare(war, null, ClientResponseFailureResource.class);
   }});

   @Before
   public void init() {
      client = (QuarkusRestClient)ClientBuilder.newClient();
   }

   @After
   public void after() throws Exception {
      client.close();
   }

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, ClientResponseFailureTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Client sends async GET requests thru client proxy. The NotFoundException should be thrown as response.
    * @tpPassCrit Exception NotFoundException is thrown
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testStreamStillOpen() throws Exception {

      final ClientResponseFailureResourceInterface proxy = client.target(generateURL(""))
            .proxy(ClientResponseFailureResourceInterface.class);
      boolean failed = true;
      try {
         proxy.error();
         failed = false;
      } catch (NotFoundException e) {
         Assert.assertEquals(HttpResponseCodes.SC_NOT_FOUND, e.getResponse().getStatus());
         Assert.assertEquals("There wasn't expected message", e.getResponse().readEntity(String.class),
            "there was an error");
         e.getResponse().close();
      }

      Assert.assertTrue("The expected NotFoundException didn't happened", failed);
   }
}
