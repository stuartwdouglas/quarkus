package io.quarkus.rest.test.providers.jaxb;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import io.quarkus.rest.test.providers.jaxb.resource.StringCharsetResource;
import io.quarkus.rest.test.providers.jaxb.resource.StringCharsetRespond;
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
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class StringCharsetTest {

   private final Logger logger = Logger.getLogger(ExceptionMapperJaxbTest.class.getName());
   static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(StreamResetTest.class);
      return TestUtil.finishContainerPrepare(war, null, StringCharsetResource.class, StringCharsetRespond.class);
   }});

   @Before
   public void init() {
      client = (QuarkusRestClient)ClientBuilder.newClient();
   }

   @After
   public void after() throws Exception {
      client.close();
      client = null;
   }

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, StringCharsetTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Tests jaxb with combination of request specified charset
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testIt() throws Exception {
      ResteasyWebTarget target = client.target(generateURL("/charset/test.xml"));
      String response = target.request().header("Accept", "application/xml;charset=iso-8859-2").get(String.class);
      logger.info(response);
      Assert.assertTrue("Response doesn't contain expected characters",
            response.contains("Test " + (char) 353 + (char) 273 + (char) 382 + (char) 269));
   }
}
