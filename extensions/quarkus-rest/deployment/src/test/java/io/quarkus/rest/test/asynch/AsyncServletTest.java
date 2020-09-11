package io.quarkus.rest.test.asynch;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.asynch.resource.AsyncServletResource;
import org.jboss.resteasy.spi.HttpResponseCodes;
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

import javax.ws.rs.core.Response;


/**
 * @tpSubChapter Asynchronous RESTEasy
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for asyncHttpServlet module
 * @tpSince RESTEasy 3.0.16
 */
public class AsyncServletTest {

   static QuarkusRestClient client;

   @Before
   public void init() {
      client = (QuarkusRestClient)ClientBuilder.newClient();
   }

   @After
   public void after() throws Exception {
      client.close();
   }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, AsyncServletResource.class);
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, AsyncServletTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Test for correct response
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testAsync() throws Exception {
      Response response = client.target(generateURL("/async")).request().get();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      Assert.assertEquals("Wrong response content", "hello", response.readEntity(String.class));
   }

   /**
    * @tpTestDetails Service unavailable test
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testTimeout() throws Exception {
      Response response = client.target(generateURL("/async/timeout")).request().get();
      Assert.assertEquals(HttpResponseCodes.SC_SERVICE_UNAVAILABLE, response.getStatus());
   }
}
