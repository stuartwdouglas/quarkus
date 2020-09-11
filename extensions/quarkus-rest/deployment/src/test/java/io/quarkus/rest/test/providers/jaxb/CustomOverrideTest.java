package io.quarkus.rest.test.providers.jaxb;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import io.quarkus.rest.test.providers.jaxb.resource.CustomOverrideWriter;
import io.quarkus.rest.test.providers.jaxb.resource.CustomOverrideResource;
import io.quarkus.rest.test.providers.jaxb.resource.CustomOverrideFoo;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import io.quarkus.rest.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.rest.test.simple.TestUtil;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class CustomOverrideTest {

   private static Logger logger = Logger.getLogger(CustomOverrideTest.class.getName());
   static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, CustomOverrideResource.class, CustomOverrideWriter.class,
            CustomOverrideFoo.class);
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
      return PortProviderUtil.generateURL(path, CustomOverrideTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Test for same resource path for media type xml and "text/x-vcard" with custom MessageBodyWriter
    * @tpInfo RESTEASY-510
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testRegression() throws Exception {
      ResteasyWebTarget target = client.target(generateURL("/test"));
      String response = target.request().accept("text/x-vcard").get(String.class);
      logger.info(response);
      Assert.assertEquals("---bill---", response);

      response = target.request().accept("application/xml").get(String.class);
      Assert.assertTrue(response.contains("customOverrideFoo"));
      logger.info(response);
   }
}
