package io.quarkus.rest.test.validation;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.api.validation.ResteasyConstraintViolation;
import org.jboss.resteasy.api.validation.ResteasyViolationException;
import org.jboss.resteasy.api.validation.Validation;
import org.jboss.resteasy.api.validation.ViolationReport;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import org.jboss.resteasy.plugins.validation.ResteasyViolationExceptionImpl;

import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.validation.resource.ValidationCoreFoo;
import io.quarkus.rest.test.validation.resource.ValidationCoreFooConstraint;
import io.quarkus.rest.test.validation.resource.ValidationCoreFooReaderWriter;
import io.quarkus.rest.test.validation.resource.ValidationCoreFooValidator;
import io.quarkus.rest.test.validation.resource.ValidationCoreClassConstraint;
import io.quarkus.rest.test.validation.resource.ValidationCoreClassValidator;
import io.quarkus.rest.test.validation.resource.ValidationCoreResourceWithAllViolationTypes;
import io.quarkus.rest.test.validation.resource.ValidationCoreResourceWithReturnValues;
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

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.Iterator;

/**
 * @tpSubChapter Response
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-923
 * @tpSince RESTEasy 3.0.16
 */
public class ValidationCoreTest {
   static final String RESPONSE_ERROR_MSG = "Response has wrong content";
   static final String WRONG_ERROR_MSG = "Expected validation error is not in response";
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

   @Before
   public void init() {
      client = (QuarkusRestClient)ClientBuilder.newClient().register(ValidationCoreFooReaderWriter.class);
   }

   @After
   public void after() throws Exception {
      client.close();
   }

   private static String generateURL(String path) {
      return PortProviderUtil.generateURL(path, ValidationCoreTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Test native, imposed and both validation of return values. Also test negative scenarios.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testReturnValues() throws Exception {
      ValidationCoreFoo foo = new ValidationCoreFoo("a");
      Assert.assertNotNull(client);
      Response response = client.target(generateURL("/return/native")).request().post(Entity.entity(foo, "application/foo"));
      // Valid native constraint
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      Assert.assertEquals(RESPONSE_ERROR_MSG, foo, response.readEntity(ValidationCoreFoo.class));
      response.close();

      // Valid imposed constraint
      foo = new ValidationCoreFoo("abcde");
      response = client.target(generateURL("/return/imposed")).request().post(Entity.entity(foo, "application/foo"));
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      Assert.assertEquals(RESPONSE_ERROR_MSG, foo, response.readEntity(ValidationCoreFoo.class));
      response.close();

      // Valid native and imposed constraints.
      foo = new ValidationCoreFoo("abc");
      response = client.target(generateURL("/return/nativeAndImposed")).request().post(Entity.entity(foo, "application/foo"));
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      Assert.assertEquals(RESPONSE_ERROR_MSG, foo, response.readEntity(ValidationCoreFoo.class));
      response.close();

      {
         // Invalid native constraint
         response = client.target(generateURL("/return/native")).request().post(Entity.entity(new ValidationCoreFoo("abcdef"), "application/foo"));
         String entity = response.readEntity(String.class);
         Assert.assertEquals(HttpResponseCodes.SC_INTERNAL_SERVER_ERROR, response.getStatus());
         String header = response.getHeaderString(Validation.VALIDATION_HEADER);
         Assert.assertNotNull("Validation header is missing", header);
         Assert.assertTrue("Wrong validation header", Boolean.valueOf(header));
         ResteasyViolationException e = new ResteasyViolationExceptionImpl(entity);
         ResteasyConstraintViolation violation = e.getReturnValueViolations().iterator().next();
         Assert.assertTrue(WRONG_ERROR_MSG, violation.getMessage().equals("s must have length: 1 <= length <= 3"));
         Assert.assertEquals(WRONG_ERROR_MSG, "ValidationCoreFoo[abcdef]", violation.getValue());
         response.close();
      }

      {
         // Invalid imposed constraint
         response = client.target(generateURL("/return/imposed")).request().post(Entity.entity(new ValidationCoreFoo("abcdef"), "application/foo"));
         Assert.assertEquals(HttpResponseCodes.SC_INTERNAL_SERVER_ERROR, response.getStatus());
         String header = response.getHeaderString(Validation.VALIDATION_HEADER);
         Assert.assertNotNull("Validation header is missing", header);
         Assert.assertTrue("Wrong validation header", Boolean.valueOf(header));
         String entity = response.readEntity(String.class);
         ViolationReport r = new ViolationReport(entity);
         TestUtil.countViolations(r, 0, 0, 0, 1);
         ResteasyConstraintViolation violation = r.getReturnValueViolations().iterator().next();
         Assert.assertTrue(WRONG_ERROR_MSG, violation.getMessage().equals("s must have length: 3 <= length <= 5"));
         Assert.assertEquals(WRONG_ERROR_MSG, "ValidationCoreFoo[abcdef]", violation.getValue());
         response.close();
      }

      {
         // Invalid native and imposed constraints
         response = client.target(generateURL("/return/nativeAndImposed")).request().post(Entity.entity(new ValidationCoreFoo("abcdef"), "application/foo"));
         Assert.assertEquals(HttpResponseCodes.SC_INTERNAL_SERVER_ERROR, response.getStatus());
         String header = response.getHeaderString(Validation.VALIDATION_HEADER);
         Assert.assertNotNull("Validation header is missing", header);
         Assert.assertTrue("Wrong validation header", Boolean.valueOf(header));
         String entity = response.readEntity(String.class);
         ViolationReport r = new ViolationReport(entity);
         TestUtil.countViolations(r, 0, 0, 0, 2);
         Iterator<ResteasyConstraintViolation> it = r.getReturnValueViolations().iterator();
         ResteasyConstraintViolation cv1 = it.next();
         ResteasyConstraintViolation cv2 = it.next();
         if (cv1.getMessage().indexOf('1') < 0) {
            ResteasyConstraintViolation temp = cv1;
            cv1 = cv2;
            cv2 = temp;
         }
         Assert.assertTrue(WRONG_ERROR_MSG, cv1.getMessage().equals("s must have length: 1 <= length <= 3"));
         Assert.assertEquals(WRONG_ERROR_MSG, "ValidationCoreFoo[abcdef]", cv1.getValue());
         Assert.assertTrue(WRONG_ERROR_MSG, cv2.getMessage().equals("s must have length: 3 <= length <= 5"));
         Assert.assertEquals(WRONG_ERROR_MSG, "ValidationCoreFoo[abcdef]", cv2.getValue());
      }
   }

   /**
    * @tpTestDetails Test violations before returning some value.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testViolationsBeforeReturnValue() throws Exception {
      // Valid
      ValidationCoreFoo foo = new ValidationCoreFoo("pqrs");
      Response response = client.target(generateURL("/all/abc/wxyz")).request().post(Entity.entity(foo, "application/foo"));
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      Assert.assertEquals(RESPONSE_ERROR_MSG, foo, response.readEntity(ValidationCoreFoo.class));
      response.close();

      // Invalid: Should have 1 each of field, property, class, and parameter violations,
      //          and no return value violations.
      foo = new ValidationCoreFoo("p");
      response = client.target(generateURL("/all/a/z")).request().post(Entity.entity(foo, "application/foo"));
      Assert.assertEquals(HttpResponseCodes.SC_BAD_REQUEST, response.getStatus());
      String header = response.getHeaderString(Validation.VALIDATION_HEADER);
      Assert.assertNotNull("Validation header is missing", header);
      Assert.assertTrue("Wrong validation header", Boolean.valueOf(header));
      String entity = response.readEntity(String.class);
      ViolationReport r = new ViolationReport(entity);
      TestUtil.countViolations(r, 2, 1, 1, 0);
      ResteasyConstraintViolation violation = TestUtil.getViolationByMessage(r.getPropertyViolations(), "size must be between 2 and 4");
      Assert.assertNotNull(WRONG_ERROR_MSG, violation);
      Assert.assertEquals(WRONG_ERROR_MSG, "a", violation.getValue());
      violation = TestUtil.getViolationByMessage(r.getPropertyViolations(), "size must be between 3 and 5");
      Assert.assertNotNull(WRONG_ERROR_MSG, violation);
      Assert.assertEquals(WRONG_ERROR_MSG, "z", violation.getValue());
      violation = r.getClassViolations().iterator().next();
      Assert.assertEquals(WRONG_ERROR_MSG, "Concatenation of s and t must have length > 5", violation.getMessage());
      Assert.assertTrue(WRONG_ERROR_MSG, violation.getValue().startsWith("io.quarkus.rest.test.validation.resource.ValidationCoreResourceWithAllViolationTypes@"));
      violation = r.getParameterViolations().iterator().next();
      Assert.assertEquals(WRONG_ERROR_MSG, "s must have length: 3 <= length <= 5", violation.getMessage());
      Assert.assertEquals(WRONG_ERROR_MSG, "ValidationCoreFoo[p]", violation.getValue());
      response.close();
   }
}
