package io.quarkus.rest.test.client.exception;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.client.exception.resource.UnauthorizedExceptionInterface;
import io.quarkus.rest.test.client.exception.resource.UnauthorizedExceptionResource;
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

import javax.ws.rs.NotAuthorizedException;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpSince RESTEasy 3.0.16
 * @tpTestCaseDetails Regression test for RESTEASY-435
 */
public class UnauthorizedExceptionTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(UnauthorizedExceptionInterface.class);
      return TestUtil.finishContainerPrepare(war, null, UnauthorizedExceptionResource.class);
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, UnauthorizedExceptionTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Check thrown exception on client side.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testMe() throws Exception {
      QuarkusRestClient client = (QuarkusRestClient)ClientBuilder.newClient();
      UnauthorizedExceptionInterface proxy = client.target(generateURL("")).proxy(UnauthorizedExceptionInterface.class);
      try {
         proxy.postIt("hello");
         Assert.fail();
      } catch (NotAuthorizedException e) {
         Assert.assertEquals(HttpResponseCodes.SC_UNAUTHORIZED, e.getResponse().getStatus());
      }
      client.close();
   }

}
