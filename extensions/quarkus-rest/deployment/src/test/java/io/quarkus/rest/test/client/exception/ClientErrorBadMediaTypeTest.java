package io.quarkus.rest.test.client.exception;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.client.exception.resource.ClientErrorBadMediaTypeHeaderDelegate;
import io.quarkus.rest.test.client.exception.resource.ClientErrorBadMediaTypeResource;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import io.quarkus.rest.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.rest.test.simple.TestUtil;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpSince RESTEasy 3.0.16
 * @tpTestCaseDetails Test client error caused by bad media type
 */
public class ClientErrorBadMediaTypeTest {

   private static Logger logger = Logger.getLogger(ClientErrorBadMediaTypeTest.class);

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(PortProviderUtil.class);
      war.addClass(TestUtil.class);
      war.addClass(ClientErrorBadMediaTypeHeaderDelegate.class);
      return TestUtil.finishContainerPrepare(war, null, ClientErrorBadMediaTypeResource.class);
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, ClientErrorBadMediaTypeTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails This test uses not defined "foo/bar" media type.
    *                Upstream variant of this test was updated by JBEAP-2594.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testBadContentType() throws Exception {
      // test
      QuarkusRestClient client = (QuarkusRestClient)ClientBuilder.newClient();
      Response response = client.target(generateURL("/")).request().post(Entity.entity("content", "foo/bar"));
      logger.info("status: " + response.getStatus());
      Assert.assertEquals(HttpResponseCodes.SC_UNSUPPORTED_MEDIA_TYPE, response.getStatus());
      response.close();
      client.close();
   }
}
