package io.quarkus.rest.test.core.basic;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.core.basic.resource.ApplicationPropertiesConfigPropertyApplicationInjection;
import io.quarkus.rest.test.core.basic.resource.ApplicationPropertiesConfigPropertyApplicationInjectionFeature;
import io.quarkus.rest.test.core.basic.resource.ApplicationPropertiesConfigPropertyApplicationInjectionResource;
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
import org.jboss.resteasy.utils.PortProviderUtil;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

/**
 * @tpSubChapter Configuration
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for custom Application class with overridden getProperties() method
 * @tpSince RESTEasy 3.0.16
 */
public class ApplicationPropertiesConfigPropertyApplicationInjectionTest {
   static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClasses(ApplicationPropertiesConfigPropertyApplicationInjection.class, ApplicationPropertiesConfigPropertyApplicationInjectionResource.class,
            ApplicationPropertiesConfigPropertyApplicationInjectionFeature.class);
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
      return PortProviderUtil.generateURL(path, ApplicationPropertiesConfigPropertyApplicationInjectionTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Test for custom Application class with overriden getProperties() method
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testApplicationPropertiesConfigApplicationInjection() {
      WebTarget target = client.target(generateURL("/getconfigproperty"));
      String response = target.queryParam("prop", "Prop1").request().get(String.class);
      Assert.assertEquals("The property is not found in the deployment", "Value1", response);
   }
}
