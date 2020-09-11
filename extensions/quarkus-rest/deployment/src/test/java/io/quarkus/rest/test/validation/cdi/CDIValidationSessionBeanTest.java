package io.quarkus.rest.test.validation.cdi;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.api.validation.ResteasyConstraintViolation;
import org.jboss.resteasy.api.validation.ResteasyViolationException;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.jboss.resteasy.plugins.validation.ResteasyViolationExceptionImpl;
import io.quarkus.rest.test.validation.cdi.resource.CDIValidationSessionBeanProxy;
import io.quarkus.rest.test.validation.cdi.resource.CDIValidationSessionBeanResource;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
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

import javax.ws.rs.client.Invocation;

import static org.junit.Assert.assertEquals;


/**
 * @tpSubChapter Response
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1008
 * @tpSince RESTEasy 3.0.16
 */
public class CDIValidationSessionBeanTest {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, CDIValidationSessionBeanResource.class);
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, CDIValidationSessionBeanTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Check for invalid parameter
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testInvalidParam() throws Exception {
      QuarkusRestClient client = (QuarkusRestClient)ClientBuilder.newClient();
      Invocation.Builder request = client.target(generateURL("/test/resource/0")).request();
      ClientResponse response = (ClientResponse) request.get();
      String answer = response.readEntity(String.class);
      assertEquals(HttpResponseCodes.SC_BAD_REQUEST, response.getStatus());
      ResteasyViolationException e = new ResteasyViolationExceptionImpl(String.class.cast(answer));
      TestUtil.countViolations(e, 1, 0, 0, 1, 0);
      ResteasyConstraintViolation cv = e.getParameterViolations().iterator().next();
      Assert.assertTrue("Expected validation error is not in response", cv.getMessage().equals("must be greater than or equal to 7"));
      client.close();
   }
}
