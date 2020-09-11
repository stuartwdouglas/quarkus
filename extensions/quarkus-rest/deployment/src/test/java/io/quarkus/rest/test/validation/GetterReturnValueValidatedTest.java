package io.quarkus.rest.test.validation;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.api.validation.ResteasyViolationException;
import org.jboss.resteasy.api.validation.Validation;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import org.jboss.resteasy.plugins.validation.ResteasyViolationExceptionImpl;

import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.validation.resource.GetterReturnValueValidatedResourceResetCount;
import io.quarkus.rest.test.validation.resource.GetterReturnValueValidatedResourceWithGetterViolation;
import io.quarkus.rest.test.validation.resource.ValidationCoreFooReaderWriter;
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
 * @tpSubChapter Response
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for getter return value validation
 * @tpSince RESTEasy 3.0.16
 */
public class GetterReturnValueValidatedTest {
   QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, (Class<?>[]) null);
   }});

   private static String generateURL(String path) {
      return PortProviderUtil.generateURL(path, GetterReturnValueValidatedTest.class.getSimpleName());
   }

   @Before
   public void init() {
      client = (QuarkusRestClient)ClientBuilder.newClient().register(ValidationCoreFooReaderWriter.class);
   }

   @After
   public void after() throws Exception {
      client.close();
   }

   /**
    * @tpTestDetails Validation of getter return value is expected because of specific validation.xml file.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testReturnValues() throws Exception {
      Response response = client.target(generateURL("/get")).request().get();
      response.close();

      response = client.target(generateURL("/set")).request().get();
      Assert.assertEquals(HttpResponseCodes.SC_NO_CONTENT, response.getStatus());
      response.close();

      // Valid native constraint
      response = client.target(generateURL("/get")).request().get();
      Assert.assertEquals(HttpResponseCodes.SC_INTERNAL_SERVER_ERROR, response.getStatus());
      String header = response.getHeaderString(Validation.VALIDATION_HEADER);
      Assert.assertNotNull("Missing validation header", header);
      Assert.assertTrue("Wrong value of validation header", Boolean.valueOf(header));
      String entity = response.readEntity(String.class);
      ResteasyViolationException e = new ResteasyViolationExceptionImpl(entity);
      TestUtil.countViolations(e, 1, 0, 0, 0, 1);
      response.close();
   }
}
