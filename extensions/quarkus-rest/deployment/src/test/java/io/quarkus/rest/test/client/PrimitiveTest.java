package io.quarkus.rest.test.client;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.client.resource.PrimitiveResource;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
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
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class PrimitiveTest extends ClientTestBase{

   static Client client;

   @BeforeClass
   public static void setup() {
      client = ClientBuilder.newClient();
   }

   @AfterClass
   public static void close() {
      client.close();
   }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, PrimitiveResource.class);
   }});

   /**
    * @tpTestDetails Client sends POST request with text entity, server sends echoes to value and returns int.
    * @tpPassCrit Correct response is returned from the server
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testInt() {
      Response response = client.target(generateURL("/int")).request().post(Entity.text("5"));
      Assert.assertEquals(200, response.getStatus());
      Assert.assertEquals("5", response.readEntity(String.class));
      response.close();
   }

   /**
    * @tpTestDetails Client sends POST request with text entity, server sends echoes to value and returns boolean.
    * @tpPassCrit Correct response is returned from the server
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testBoolean() {
      Response response = client.target(generateURL("/boolean")).request().post(Entity.text("true"));
      Assert.assertEquals(200, response.getStatus());
      Assert.assertEquals("true", response.readEntity(String.class));
      response.close();
   }

   /**
    * @tpTestDetails Client sends GET request, server sends Accepted response
    * @tpPassCrit Correct response is returned from the server
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testBuildResponse() {
      Response response = client.target(generateURL("/nothing")).request().get();
      Assert.assertEquals("", response.readEntity(String.class));
      Assert.assertEquals(200, response.getStatus());
      response.close();
   }
}
