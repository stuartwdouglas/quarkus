package io.quarkus.rest.test.validation.ejb;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.api.validation.Validation;
import org.jboss.resteasy.api.validation.ViolationReport;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.rest.test.validation.ejb.resource.EJBParameterViolationsOnlyDataObject;
import io.quarkus.rest.test.validation.ejb.resource.EJBParameterViolationsOnlySingletonResource;
import io.quarkus.rest.test.validation.ejb.resource.EJBParameterViolationsOnlyStatefulResource;
import io.quarkus.rest.test.validation.ejb.resource.EJBParameterViolationsOnlyStatelessResource;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Test situation where EJBs have parameter violations but no class, field, or property violations.
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-2503
 * @tpSince RESTEasy 4.5
 */
public class EJBParameterViolationsOnlyTest {

    private static QuarkusRestClient client;
    private static EJBParameterViolationsOnlyDataObject validDataObject;
    private static EJBParameterViolationsOnlyDataObject invalidDataObject;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null,
                            EJBParameterViolationsOnlyStatelessResource.class,
                            EJBParameterViolationsOnlyStatefulResource.class,
                            EJBParameterViolationsOnlySingletonResource.class);
                }
            });

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, EJBParameterViolationsOnlyTest.class.getSimpleName());
    }

    @BeforeClass
    public static void beforeClass() {
        client = (QuarkusRestClient) ClientBuilder.newClient();

        // Create valid data object.
        validDataObject = new EJBParameterViolationsOnlyDataObject();
        validDataObject.setDirection("north");
        validDataObject.setSpeed(10);

        // Create data object with constraint violations.
        invalidDataObject = new EJBParameterViolationsOnlyDataObject();
        invalidDataObject.setDirection("north");
        invalidDataObject.setSpeed(0);
    }

    @AfterClass
    public static void afterClass() {
        client.close();
    }

    /**
     * @tpTestDetails Run tests for stateless EJB
     * @tpSince RESTEasy 4.5
     */
    @Test
    public void testStateless() throws Exception {
        doValidationTest(client.target(generateURL("/app/stateless")));
    }

    /**
     * @tpTestDetails Run tests for stateful EJB
     * @tpSince RESTEasy 4.5
     */
    @Test
    public void testStateful() throws Exception {
        doValidationTest(client.target(generateURL("/app/stateful")));
    }

    /**
     * @tpTestDetails Run tests for singleton EJB
     * @tpSince RESTEasy 4.5
     */
    @Test
    public void testSingleton() throws Exception {
        doValidationTest(client.target(generateURL("/app/singleton")));
    }

    void doValidationTest(WebTarget target) throws Exception {
        // Invoke with valid data object.
        Invocation.Builder request = target.path("validation").request().accept(MediaType.TEXT_PLAIN);
        Response response = request.post(Entity.entity(validDataObject, MediaType.APPLICATION_JSON), Response.class);
        Assert.assertEquals(200, response.getStatus());

        // Reset flag indicating method has been executed.
        boolean used = target.path("used").request().get(boolean.class);
        Assert.assertTrue(used);
        target.path("reset").request().get();

        // Invoke with invalid data object.
        Response response2 = request.post(Entity.entity(invalidDataObject, MediaType.APPLICATION_JSON), Response.class);
        Assert.assertEquals(400, response2.getStatus());
        Assert.assertEquals("true", response2.getHeaderString(Validation.VALIDATION_HEADER));
        ViolationReport report = response2.readEntity(ViolationReport.class);
        TestUtil.countViolations(report, 0, 0, 1, 0);
        used = target.path("used").request().get(boolean.class);
        Assert.assertFalse(used);
    }
}
