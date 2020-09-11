package io.quarkus.rest.test.resource.param;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.resource.param.resource.SpecialCharsInUrlResource;
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
 * @tpSubChapter Parameters
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for special characters in url
 * @tpSince RESTEasy 3.0.16
 */
public class SpecialCharsInUrlTest {

   static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, SpecialCharsInUrlResource.class);
   }});

   @BeforeClass
   public static void setup() {
      client = ClientBuilder.newClient();
   }

   @AfterClass
   public static void cleanup() {
      client.close();
   }

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, SpecialCharsInUrlTest.class.getSimpleName());
   }

   private static final String encodedPart = "foo+bar%20gee@foo.com";
   private static final String decodedPart = "foo+bar gee@foo.com";

   /**
    * @tpTestDetails Test for '+' and '@' characters in url, RESTEASY-137
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testGet() throws Exception {
      WebTarget target = client.target(String.format("%s%s?foo=%s", generateURL("/simple/"), encodedPart, encodedPart));
      Response response = target.request().get();
      Assert.assertEquals("The result is not correctly decoded", HttpResponseCodes.SC_OK, response.getStatus());
      String result = response.readEntity(String.class);
      Assert.assertEquals("The result is not correctly decoded", decodedPart, result);
   }

}
