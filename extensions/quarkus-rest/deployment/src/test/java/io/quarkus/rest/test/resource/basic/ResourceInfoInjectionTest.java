package io.quarkus.rest.test.resource.basic;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.resource.basic.resource.ResourceInfoInjectionFilter;
import io.quarkus.rest.test.resource.basic.resource.ResourceInfoInjectionResource;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.utils.PortProviderUtil;
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
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

/**
 * @tpSubChapter Resources
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for JBEAP-4701
 * @tpSince RESTEasy 3.0.17
 */
public class ResourceInfoInjectionTest {
   protected static Client client;

   @BeforeClass
   public static void init() {
      client = ClientBuilder.newClient();
   }

   @AfterClass
   public static void close() {
      client.close();
   }

   private static String generateURL(String path) {
      return PortProviderUtil.generateURL(path, ResourceInfoInjectionTest.class.getSimpleName());
   }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, ResourceInfoInjectionFilter.class,
            ResourceInfoInjectionResource.class);
   }});

   /**
    * @tpTestDetails Check for injecting ResourceInfo object in ContainerResponseFilter
    * @tpSince RESTEasy 3.0.17
    */
   @Test
   public void testNotFound() throws Exception {
      WebTarget target = client.target(generateURL("/bogus"));
      Response response = target.request().get();
      String entity = response.readEntity(String.class);
      Assert.assertEquals("ResponseFilter was probably not applied to response", HttpResponseCodes.SC_NOT_FOUND * 2, response.getStatus());
      Assert.assertTrue("Wrong body of response",  entity.contains("RESTEASY003210"));
   }

   /**
    * @tpTestDetails Check for injecting ResourceInfo object in end-point
    * @tpSince RESTEasy 3.0.17
    */
   @Test
   public void testAsync() throws Exception {
      WebTarget target = client.target(generateURL("/async"));
      Response response = target.request().post(Entity.entity("hello", "text/plain"));
      String val = response.readEntity(String.class);
      Assert.assertEquals("OK status is expected", HttpResponseCodes.SC_OK, response.getStatus());
      Assert.assertEquals("Wrong body of response", "async", val);
   }
}
