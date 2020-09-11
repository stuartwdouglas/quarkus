package io.quarkus.rest.test.resource.path;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.resource.path.resource.MultipleMatrixSegmentsResource;
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

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test that a locator and resource with same path params work
 * @tpSince RESTEasy 3.0.16
 */
public class MultipleMatrixSegmentsTest {

   static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, MultipleMatrixSegmentsResource.class);
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
      return PortProviderUtil.generateURL(path, MultipleMatrixSegmentsTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Test segments on start and on end of path
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testMultipleStartAndEnd() throws Exception {
      Response response = client.target(generateURL("/;name=bill;ssn=111/children/;name=skippy;ssn=3344")).request().get();
      Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
      response.close();
   }

   /**
    * @tpTestDetails Test segments in the middle of path
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testMultipleMiddle() throws Exception {
      Response response = client.target(generateURL("/stuff/;name=first;ssn=111/;name=second;ssn=3344/first")).request().get();
      Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
      response.close();
   }
}
