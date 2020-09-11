package io.quarkus.rest.test.validation.cdi;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.api.validation.ViolationReport;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.rest.test.validation.cdi.resource.ApplicationScopeMyDto;
import io.quarkus.rest.test.validation.cdi.resource.ApplicationScopeRestServiceAppScoped;
import io.quarkus.rest.test.validation.cdi.resource.ApplicationScopeRestServiceReqScoped;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Validation
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1459
 * @tpSince RESTEasy 3.1.0.Final
 */
public class ApplicationScopeValidationTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    Map<String, String> contextParam = new HashMap<>();
                    contextParam.put(ResteasyContextParameters.RESTEASY_PREFER_JACKSON_OVER_JSONB, "true");
                    return TestUtil.finishContainerPrepare(war, contextParam, ApplicationScopeRestServiceAppScoped.class,
                            ApplicationScopeRestServiceReqScoped.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ApplicationScopeValidationTest.class.getSimpleName());
    }

    protected Client client;

    @Before
    public void beforeTest() {
        client = ClientBuilder.newClient();
    }

    @After
    public void afterTest() {
        client.close();
    }

    @Test
    public void testValidationApplicationScope() {
        WebTarget target = client.target(generateURL("/testapp/send"));
        ApplicationScopeMyDto dto = new ApplicationScopeMyDto();
        dto.setPath("path");
        dto.setTest("test");
        Response response = target.request().post(Entity.entity(dto, MediaType.APPLICATION_JSON));
        Assert.assertEquals(200, response.getStatus());
        response.close();

        response = target.request().post(Entity.entity(null, MediaType.APPLICATION_JSON));
        Assert.assertEquals(400, response.getStatus());
        Object header = response.getHeaders().getFirst(org.jboss.resteasy.api.validation.Validation.VALIDATION_HEADER);
        Assert.assertTrue(header instanceof String);
        Assert.assertTrue(Boolean.valueOf(String.class.cast(header)));
        ViolationReport report = response.readEntity(ViolationReport.class);

        // Show that server didn't call resource method, which would have caused a return value violation.
        countViolations(report, 0, 0, 1, 0);
        response.close();
    }

    @Test
    public void testValidationRequestScope() {
        WebTarget target = client.target(generateURL("/testreq/send"));
        ApplicationScopeMyDto dto = new ApplicationScopeMyDto();
        dto.setPath("path");
        dto.setTest("test");
        Response response = target.request().post(Entity.entity(dto, MediaType.APPLICATION_JSON));
        Assert.assertEquals(200, response.getStatus());
        response.close();

        response = target.request().post(Entity.entity(null, MediaType.APPLICATION_JSON));
        Assert.assertEquals(400, response.getStatus());
        Object header = response.getHeaders().getFirst(org.jboss.resteasy.api.validation.Validation.VALIDATION_HEADER);
        Assert.assertTrue(header instanceof String);
        Assert.assertTrue(Boolean.valueOf(String.class.cast(header)));
        ViolationReport report = response.readEntity(ViolationReport.class);

        // Show that server didn't call resource method, which would have caused a return value violation.
        countViolations(report, 0, 0, 1, 0);
        response.close();
    }

    private void countViolations(ViolationReport e, int propertyCount, int classCount, int parameterCount,
            int returnValueCount) {
        Assert.assertEquals(propertyCount, e.getPropertyViolations().size());
        Assert.assertEquals(classCount, e.getClassViolations().size());
        Assert.assertEquals(parameterCount, e.getParameterViolations().size());
        Assert.assertEquals(returnValueCount, e.getReturnValueViolations().size());
    }
}
