package io.quarkus.rest.test.cdi.ejb;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.api.validation.ViolationReport;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.cdi.ejb.resource.EJBCDIValidationApplication;
import io.quarkus.rest.test.cdi.ejb.resource.EJBCDIValidationSingletonResource;
import io.quarkus.rest.test.cdi.ejb.resource.EJBCDIValidationStatefulResource;
import io.quarkus.rest.test.cdi.ejb.resource.EJBCDIValidationStatelessResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails EJB, CDI, Validation, and RESTEasy integration test: RESTEASY-1749
 * @tpSince RESTEasy 4.0.0
 */
public class EJBCDIValidationTest {

    private static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(EJBCDIValidationApplication.class)
                            .addClasses(EJBCDIValidationStatelessResource.class)
                            .addClasses(EJBCDIValidationStatefulResource.class)
                            .addClasses(EJBCDIValidationSingletonResource.class)
                            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

                    return TestUtil.finishContainerPrepare(war, null);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, EJBCDIValidationTest.class.getSimpleName());
    }

    @BeforeClass
    public static void init() {
        client = ClientBuilder.newClient();
    }

    @AfterClass
    public static void close() {
        client.close();
    }

    /**
     * @tpTestDetails Verify correct order of validation on stateless EJBs
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testStateless() {
        // Expect property, parameter violations.
        WebTarget base = client.target(generateURL("/rest/stateless/"));
        Builder builder = base.path("post/n").request();
        Response response = builder.post(Entity.entity("-1", MediaType.APPLICATION_JSON_TYPE));
        Assert.assertEquals(400, response.getStatus());
        String answer = response.readEntity(String.class);
        ViolationReport r = new ViolationReport(answer);
        TestUtil.countViolations(r, 1, 0, 1, 0);

        // Valid invocation
        response = base.path("set/xyz").request().get();
        Assert.assertEquals(204, response.getStatus());
        response.close();

        // EJB resource has been created: expect parameter violation.
        builder = base.path("post/n").request();
        builder.accept(MediaType.TEXT_PLAIN_TYPE);
        response = builder.post(Entity.entity("-1", MediaType.APPLICATION_JSON_TYPE));
        Assert.assertEquals(400, response.getStatus());
        answer = response.readEntity(String.class);
        r = new ViolationReport(answer);
        TestUtil.countViolations(r, 0, 0, 1, 0);
    }

    /**
     * @tpTestDetails Verify correct order of validation on stateful EJBs
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testStateful() {
        // Expect property, parameter violations
        WebTarget base = client.target(generateURL("/rest/stateful/"));
        Builder builder = base.path("post/n").request();
        Response response = builder.post(Entity.entity("-1", MediaType.APPLICATION_JSON_TYPE));
        Assert.assertEquals(400, response.getStatus());
        String answer = response.readEntity(String.class);
        ViolationReport r = new ViolationReport(answer);
        TestUtil.countViolations(r, 1, 0, 1, 0);

        // Valid invocation
        response = base.path("set/xyz").request().get();
        Assert.assertEquals(204, response.getStatus());
        response.close();

        // EJB resource gets created again: expect property and parameter violations.
        builder = base.path("post/n").request();
        builder.accept(MediaType.TEXT_PLAIN_TYPE);
        response = builder.post(Entity.entity("-1", MediaType.APPLICATION_JSON_TYPE));
        Assert.assertEquals(400, response.getStatus());
        answer = response.readEntity(String.class);
        r = new ViolationReport(answer);
        TestUtil.countViolations(r, 1, 0, 1, 0);
    }

    /**
     * @tpTestDetails Verify correct order of validation on singleton EJBs
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testSingleton() {
        doTestSingleton(1); // Expect property violation when EJB resource gets created.
        doTestSingleton(0); // EJB resource has been created: expect no property violation.
    }

    void doTestSingleton(int propertyViolations) {
        // Expect property, parameter violations
        WebTarget base = client.target(generateURL("/rest/singleton/"));
        Builder builder = base.path("post/n").request();
        Response response = builder.post(Entity.entity("-1", MediaType.APPLICATION_JSON_TYPE));
        Assert.assertEquals(400, response.getStatus());
        String answer = response.readEntity(String.class);
        ViolationReport r = new ViolationReport(answer);
        TestUtil.countViolations(r, propertyViolations, 0, 1, 0);

        // Valid invocation
        response = base.path("set/xyz").request().get();
        Assert.assertEquals(204, response.getStatus());
        response.close();

        // EJB resource has been created: expect parameter violation.
        builder = base.path("post/n").request();
        builder.accept(MediaType.TEXT_PLAIN_TYPE);
        response = builder.post(Entity.entity("-1", MediaType.APPLICATION_JSON_TYPE));
        Assert.assertEquals(400, response.getStatus());
        answer = response.readEntity(String.class);
        r = new ViolationReport(answer);
        TestUtil.countViolations(r, 0, 0, 1, 0);
    }
}
