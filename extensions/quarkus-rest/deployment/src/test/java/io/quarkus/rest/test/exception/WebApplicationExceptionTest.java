package io.quarkus.rest.test.exception;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.exception.resource.WebApplicationExceptionResource;
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

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

/**
 * @tpSubChapter Exceptions
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for javax.ws.rs.WebApplicationException class
 * @tpSince RESTEasy 3.0.16
 */
public class WebApplicationExceptionTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, WebApplicationExceptionResource.class);
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, WebApplicationExceptionTest.class.getSimpleName());
   }

   private void basicTest(String path, int code) {
      QuarkusRestClient client = (QuarkusRestClient)ClientBuilder.newClient();
      WebTarget base = client.target(generateURL(path));
      Response response = base.request().get();
      Assert.assertEquals(code, response.getStatus());
      response.close();
      client.close();
   }

   /**
    * @tpTestDetails Test for exception without error entity
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testException() {
      basicTest("/exception", HttpResponseCodes.SC_UNAUTHORIZED);
   }

   /**
    * @tpTestDetails Test for exception with error entity.
    *                Regression test for RESTEASY-24
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testExceptionWithEntity() {
      basicTest("/exception/entity", HttpResponseCodes.SC_UNAUTHORIZED);
   }

}
