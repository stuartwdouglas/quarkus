package io.quarkus.rest.test.cdi.basic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.cdi.basic.resource.SingletonLocalIF;
import io.quarkus.rest.test.cdi.basic.resource.SingletonRootResource;
import io.quarkus.rest.test.cdi.basic.resource.SingletonSubResource;
import io.quarkus.rest.test.cdi.basic.resource.SingletonTestBean;
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
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for Singleton beans
 * @tpSince RESTEasy 3.0.16
 */
public class SingletonTest {
   static Client client;
   protected static final Logger logger = LogManager.getLogger(SingletonTest.class.getName());

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

      war.addClasses(SingletonLocalIF.class, SingletonSubResource.class,
            SingletonTestBean.class);
      return TestUtil.finishContainerPrepare(war, null, SingletonRootResource.class);
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, SingletonTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Three requests for singleton bean
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testSingleton() throws Exception {
      WebTarget base = client.target(generateURL("/root"));
      String value = base.path("sub").request().get(String.class);
      Assert.assertEquals("Wrong content of response", "hello", value);
      value = base.path("injected").request().get(String.class);
      Assert.assertEquals("Wrong content of response", "true", value);
      value = base.path("intfsub").request().get(String.class);
      logger.info(value);
      Response response = base.path("exception").request().get();
      Assert.assertEquals(HttpResponseCodes.SC_CREATED, response.getStatus());
   }

}
