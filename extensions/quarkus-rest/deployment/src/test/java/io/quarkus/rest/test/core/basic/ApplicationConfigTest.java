package io.quarkus.rest.test.core.basic;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.core.basic.resource.ApplicationConfig;
import io.quarkus.rest.test.core.basic.resource.ApplicationConfigInjectionResource;
import io.quarkus.rest.test.core.basic.resource.ApplicationConfigInterface;
import io.quarkus.rest.test.core.basic.resource.ApplicationConfigQuotedTextWriter;
import io.quarkus.rest.test.core.basic.resource.ApplicationConfigResource;
import io.quarkus.rest.test.core.basic.resource.ApplicationConfigService;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
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
import javax.ws.rs.core.Response;

/**
 * @tpSubChapter Configuration
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for custom Application class
 * @tpSince RESTEasy 3.0.16
 */
public class ApplicationConfigTest {

   static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClasses(ApplicationConfig.class, ApplicationConfigInjectionResource.class, ApplicationConfigInterface.class,
            ApplicationConfigQuotedTextWriter.class, ApplicationConfigResource.class,
            ApplicationConfigService.class);
      return war;
   }});

   @BeforeClass
   public static void init() {
      client = ClientBuilder.newClient();
   }

   @AfterClass
   public static void after() throws Exception {
      client.close();
   }

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, ApplicationConfigTest.class.getSimpleName());
   }

   private void basicTest(String uri, String body) {
      Response response = client.target(uri).request().get();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      response.close();
   }

   /**
    * @tpTestDetails Test base resource
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testIt() {
      basicTest(generateURL("/my"), "\"hello\"");
      basicTest(generateURL("/myinterface"), "hello");
   }

   /**
    * @tpTestDetails Injection test
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testFieldInjection() {
      basicTest(generateURL("/injection/field"), "true");
   }

   /**
    * @tpTestDetails Setter injection test
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testSetterInjection() {
      basicTest(generateURL("/injection/setter"), "true");
   }

   /**
    * @tpTestDetails Setter injection test
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testConstructorInjection() {
      basicTest(generateURL("/injection/constructor"), "true");
   }

}
