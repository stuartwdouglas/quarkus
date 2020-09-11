package io.quarkus.rest.test.validation;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.api.validation.ResteasyConstraintViolation;
import org.jboss.resteasy.api.validation.ResteasyViolationException;
import org.jboss.resteasy.api.validation.Validation;
import org.jboss.resteasy.plugins.validation.ResteasyViolationExceptionImpl;
import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.rest.test.validation.resource.ValidationCoreFoo;
import io.quarkus.rest.test.validation.resource.ValidationCoreFooReaderWriter;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Response
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test basic validation with disable executable-validation in validation.xml file. Validation should not be
 *                    active.
 * @tpSince RESTEasy 3.0.16
 */
public class ExecutableValidationDisabledTest {
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
                }
            });

    @Before
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient().register(ValidationCoreFooReaderWriter.class);
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ExecutableValidationDisabledTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test disabled validation of returned value.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testReturnValues() throws Exception {
        // Valid native constraint
        ValidationCoreFoo foo = new ValidationCoreFoo("a");
        Response response = client.target(generateURL("/return/native")).request().post(Entity.entity(foo, "application/foo"));
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals(RESPONSE_ERROR_MSG, foo, response.readEntity(ValidationCoreFoo.class));
        response.close();

        // Valid imposed constraint
        foo = new ValidationCoreFoo("abcde");
        response = client.target(generateURL("/return/imposed")).request().post(Entity.entity(foo, "application/foo"));
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals(RESPONSE_ERROR_MSG, foo, response.readEntity(ValidationCoreFoo.class));
        response.close();

        // Valid native and imposed constraints.
        foo = new ValidationCoreFoo("abc");
        response = client.target(generateURL("/return/nativeAndImposed")).request().post(Entity.entity(foo, "application/foo"));
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals(RESPONSE_ERROR_MSG, foo, response.readEntity(ValidationCoreFoo.class));
        response.close();

        {
            // Invalid native constraint
            // BUT EXECUTABLE VALIDATION IS DISABLE.
            foo = new ValidationCoreFoo("abcdef");
            response = client.target(generateURL("/return/native")).request().post(Entity.entity(foo, "application/foo"));
            Assert.assertEquals(Status.OK, response.getStatus());
            Assert.assertEquals(RESPONSE_ERROR_MSG, foo, response.readEntity(ValidationCoreFoo.class));
            response.close();
        }

        {
            // Invalid imposed constraint
            // BUT EXECUTABLE VALIDATION IS DISABLE.
            foo = new ValidationCoreFoo("abcdef");
            response = client.target(generateURL("/return/imposed")).request().post(Entity.entity(foo, "application/foo"));
            Assert.assertEquals(Status.OK, response.getStatus());
            Assert.assertEquals(RESPONSE_ERROR_MSG, foo, response.readEntity(ValidationCoreFoo.class));
            response.close();
        }

        {
            // Invalid native and imposed constraints
            // BUT EXECUTABLE VALIDATION IS DISABLE.
            foo = new ValidationCoreFoo("abcdef");
            response = client.target(generateURL("/return/nativeAndImposed")).request()
                    .post(Entity.entity(foo, "application/foo"));
            Assert.assertEquals(Status.OK, response.getStatus());
            Assert.assertEquals(RESPONSE_ERROR_MSG, foo, response.readEntity(ValidationCoreFoo.class));
            response.close();
        }
    }

    /**
     * @tpTestDetails Test disabled validation before return value evaluation.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testViolationsBeforeReturnValue() throws Exception {
        // Valid
        ValidationCoreFoo foo = new ValidationCoreFoo("pqrs");
        Response response = client.target(generateURL("/all/abc/wxyz")).request().post(Entity.entity(foo, "application/foo"));
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals(RESPONSE_ERROR_MSG, foo, response.readEntity(ValidationCoreFoo.class));
        response.close();

        // Invalid: Should have 1 each of field, property, class, and parameter violations,
        //          and no return value violations.
        // BUT EXECUTABLE VALIDATION IS DISABLE. There will be no parameter violation.
        response = client.target(generateURL("/all/a/z")).request().post(Entity.entity(foo, "application/foo"));
        Assert.assertEquals(Status.BAD_REQUEST, response.getStatus());
        String header = response.getHeaderString(Validation.VALIDATION_HEADER);
        Assert.assertNotNull("Missing validation header", header);
        Assert.assertTrue("Wrong value of validation header", Boolean.valueOf(header));
        String entity = response.readEntity(String.class);
        ResteasyViolationException e = new ResteasyViolationExceptionImpl(entity);
        TestUtil.countViolations(e, 3, 2, 1, 0, 0);
        ResteasyConstraintViolation violation = TestUtil.getViolationByMessage(e.getPropertyViolations(),
                "size must be between 2 and 4");
        Assert.assertNotNull(WRONG_ERROR_MSG, violation);
        Assert.assertEquals(WRONG_ERROR_MSG, "a", violation.getValue());
        violation = TestUtil.getViolationByMessage(e.getPropertyViolations(), "size must be between 3 and 5");
        Assert.assertNotNull(WRONG_ERROR_MSG, violation);
        Assert.assertEquals(WRONG_ERROR_MSG, "z", violation.getValue());
        violation = e.getClassViolations().iterator().next();
        Assert.assertEquals(WRONG_ERROR_MSG, "Concatenation of s and t must have length > 5", violation.getMessage());
        Assert.assertTrue(WRONG_ERROR_MSG, violation.getValue()
                .startsWith("io.quarkus.rest.test.validation.resource.ValidationCoreResourceWithAllViolationTypes@"));
        response.close();
    }
}
