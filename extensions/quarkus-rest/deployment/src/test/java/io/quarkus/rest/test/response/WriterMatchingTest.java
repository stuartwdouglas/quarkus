package io.quarkus.rest.test.response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.response.resource.WriterMatchingBoolWriter;
import io.quarkus.rest.test.response.resource.WriterMatchingResource;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import io.quarkus.rest.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.rest.test.simple.TestUtil;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

/**
 * @tpSubChapter Writers
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class WriterMatchingTest {

   static Client client;


   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, WriterMatchingTest.class.getSimpleName());
   }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, WriterMatchingResource.class, WriterMatchingBoolWriter.class);
   }});

   @Before
   public void setup() {
      client = ClientBuilder.newClient();
   }

   @After
   public void cleanup() {
      client.close();
   }

   /**
    * @tpTestDetails Check correct sort of writers. RESTEasy should check correct writer.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testMatch() {
      // writers sorted by type, mediatype, and then by app over builtin
      Response response = client.target(generateURL("/bool")).request("text/plain").get();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      String data = response.readEntity(String.class);
      response.close();
      Assert.assertEquals("RESTEasy returns wrong data", data, "true");
   }

}
