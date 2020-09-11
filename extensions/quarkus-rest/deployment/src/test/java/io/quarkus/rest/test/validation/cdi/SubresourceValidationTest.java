package io.quarkus.rest.test.validation.cdi;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.api.validation.ViolationReport;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import io.quarkus.rest.test.validation.cdi.resource.SubresourceValidationQueryBeanParam;
import io.quarkus.rest.test.validation.cdi.resource.SubresourceValidationResource;
import io.quarkus.rest.test.validation.cdi.resource.SubresourceValidationSubResource;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
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
 * @tpTestCaseDetails Regression test for RESTEASY-1103
 * @tpSince RESTEasy 3.0.16
 */
public class SubresourceValidationTest {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, SubresourceValidationResource.class);
   }});

   protected Client client;

   @Before
   public void beforeTest()
   {
      client = ClientBuilder.newClient();
   }

   @After
   public void afterTest()
   {
      client.close();
      client = null;
   }

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, SubresourceValidationTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Test for subresources
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testSubresource() throws Exception {
      Invocation.Builder request = client.target(generateURL("/sub/17?limit=abcdef")).request();
      ClientResponse response = (ClientResponse) request.get();
      ViolationReport r = new ViolationReport(response.readEntity(String.class));
      TestUtil.countViolations(r, 0, 0, 2, 0);
      assertEquals(HttpResponseCodes.SC_BAD_REQUEST, response.getStatus());
   }

   /**
    * @tpTestDetails Test for validation of returned value
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testReturnValue() throws Exception {
      Invocation.Builder request = client.target(generateURL("/sub/return/abcd")).request();
      ClientResponse response = (ClientResponse) request.get();
      ViolationReport r = new ViolationReport(response.readEntity(String.class));
      TestUtil.countViolations(r, 0, 0, 0, 1);
      assertEquals(HttpResponseCodes.SC_INTERNAL_SERVER_ERROR, response.getStatus());
   }
}
