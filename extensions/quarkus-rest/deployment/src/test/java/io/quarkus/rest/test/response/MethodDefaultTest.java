package io.quarkus.rest.test.response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.response.resource.MethodDefaultResource;
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

import javax.ws.rs.core.Response;
import java.util.HashSet;

/**
 * @tpSubChapter Response
 * @tpChapter Integration tests
 * @tpTestCaseDetails Spec requires that HEAD and OPTIONS are handled in a default manner
 * @tpSince RESTEasy 3.0.16
 */
public class MethodDefaultTest {

   static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, MethodDefaultResource.class);
   }});

   @BeforeClass
   public static void init() {
      client = (QuarkusRestClient)ClientBuilder.newClient();
   }

   @AfterClass
   public static void close() {
      client.close();
   }

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, MethodDefaultTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Client invokes Head on root resource at /GetTest;
    *                which no request method designated for HEAD;
    *                Verify that corresponding GET Method is invoked.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testHeadPlain() throws Exception {
      Response response = client.target(generateURL("/GetTest")).request().header("Accept", "text/plain").head();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      String header = response.getHeaderString("CTS-HEAD");
      Assert.assertEquals("Wrong CTS-HEAD header", "text-plain", header);
      response.close();
   }

   /**
    * @tpTestDetails Client invokes HEAD on root resource at /GetTest;
    *                which no request method designated for HEAD;
    *                Verify that corresponding GET Method is invoked.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testHeadHtml() throws Exception {
      Response response = client.target(generateURL("/GetTest")).request().header("Accept", "text/html").head();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      String header = response.getHeaderString("CTS-HEAD");
      Assert.assertEquals("Wrong CTS-HEAD header", "text-html", header);
      response.close();
   }

   /**
    * @tpTestDetails Client invokes HEAD on sub resource at /GetTest/sub;
    * which no request method designated for HEAD;
    * Verify that corresponding GET Method is invoked instead.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testHeadSubresource() throws Exception {
      Response response = client.target(generateURL("/GetTest/sub")).request().header("Accept", "text/plain").head();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      String header = response.getHeaderString("CTS-HEAD");
      Assert.assertEquals("Wrong CTS-HEAD header", "sub-text-plain", header);
      response.close();
   }

   /**
    * @tpTestDetails If client invokes OPTIONS and there is no request method that exists, verify that an automatic response is
    *                generated
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testOptions() throws Exception {

      Response response = client.target(generateURL("/GetTest/sub")).request().options();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      String allowedHeader = response.getHeaderString("Allow");
      Assert.assertNotNull("Wrong Allow header", allowedHeader);
      String[] allowed = allowedHeader.split(",");
      HashSet<String> set = new HashSet<String>();
      for (String allow : allowed) {
         set.add(allow.trim());
      }

      Assert.assertTrue("Wrong Allow header", set.contains("GET"));
      Assert.assertTrue("Wrong Allow header", set.contains("OPTIONS"));
      Assert.assertTrue("Wrong Allow header", set.contains("HEAD"));
      response.close();
   }

}
