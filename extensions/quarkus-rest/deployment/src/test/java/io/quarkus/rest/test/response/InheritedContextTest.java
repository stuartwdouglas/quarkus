package io.quarkus.rest.test.response;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.spi.HttpResponseCodes;
import io.quarkus.rest.test.response.resource.InheritedContextNewService;
import io.quarkus.rest.test.response.resource.InheritedContextNewSubService;
import io.quarkus.rest.test.response.resource.InheritedContextService;
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

/**
 * @tpSubChapter Response
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-952
 * @tpSince RESTEasy 3.0.16
 */
public class InheritedContextTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, InheritedContextService.class,
            InheritedContextNewService.class, InheritedContextNewSubService.class);
   }});

   protected Client client;

   @Before
   public void beforeTest()
   {
      client = ClientBuilder.newClient();
   }

   @After
   public void afterTest()
   {
      client.close();
      client = null;
   }

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, InheritedContextTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Test basic resource with no inheritance
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testContext() throws Exception {
      Invocation.Builder request = client.target(generateURL("/super/test/BaseService")).request();
      Response response = request.get();
      String s = response.readEntity(String.class);
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      Assert.assertEquals("true", s);
      response.close();
   }

   /**
    * @tpTestDetails Test basic resource with one level of inheritance
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testInheritedContextOneLevel() throws Exception {
      Invocation.Builder request = client.target(generateURL("/sub/test/SomeService")).request();
      Response response = request.get();
      String s = response.readEntity(String.class);
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      Assert.assertEquals("true", s);
      response.close();
   }

   /**
    * @tpTestDetails Test basic resource with two levels of inheritance
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testInheritedContextTwoLevels() throws Exception {
      Invocation.Builder request = client.target(generateURL("/subsub/test/SomeSubService")).request();
      Response response = request.get();
      String s = response.readEntity(String.class);
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      Assert.assertEquals("true", s);
      response.close();
   }
}
