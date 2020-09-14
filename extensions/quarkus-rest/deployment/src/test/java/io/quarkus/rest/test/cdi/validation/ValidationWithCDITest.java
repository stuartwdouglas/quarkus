package io.quarkus.rest.test.cdi.validation;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.resteasy.api.validation.Validation;
import org.jboss.resteasy.api.validation.ViolationReport;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.quarkus.rest.test.cdi.validation.resource.AbstractAsyncRootResource;
import io.quarkus.rest.test.cdi.validation.resource.AsyncRootResource;
import io.quarkus.rest.test.cdi.validation.resource.AsyncRootResourceImpl;
import io.quarkus.rest.test.cdi.validation.resource.AsyncSubResource;
import io.quarkus.rest.test.cdi.validation.resource.AsyncSubResourceImpl;
import io.quarkus.rest.test.cdi.validation.resource.AsyncValidResource;
import io.quarkus.rest.test.cdi.validation.resource.QueryBeanParam;
import io.quarkus.rest.test.cdi.validation.resource.QueryBeanParamImpl;
import io.quarkus.rest.test.cdi.validation.resource.RootResource;
import io.quarkus.rest.test.cdi.validation.resource.RootResourceImpl;
import io.quarkus.rest.test.cdi.validation.resource.SubResource;
import io.quarkus.rest.test.cdi.validation.resource.SubResourceImpl;
import io.quarkus.rest.test.cdi.validation.resource.TestApplication;
import io.quarkus.rest.test.cdi.validation.resource.ValidResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails Tests RESTEASY-1186, which reports issues with validation in
 *                    the presence of CDI.
 * @tpSince RESTEasy 3.1.0
 */
public class ValidationWithCDITest {
    @Deployment(testable = false)
    public static Archive<?> createTestArchive() {
        WebArchive war = TestUtil.prepareArchive(ValidationWithCDITest.class.getSimpleName());
        war.addClasses(TestApplication.class)
                .addClasses(QueryBeanParam.class, QueryBeanParamImpl.class)
                .addClasses(RootResource.class, RootResourceImpl.class, ValidResource.class)
                .addClasses(SubResource.class, SubResourceImpl.class)
                .addClass(AbstractAsyncRootResource.class)
                .addClasses(AsyncRootResource.class, AsyncRootResourceImpl.class)
                .addClasses(AsyncSubResource.class, AsyncSubResourceImpl.class)
                .addClasses(AsyncValidResource.class)

                .addAsWebInfResource(ValidationWithCDITest.class.getPackage(), "web.xml", "/web.xml");
        return TestUtil.finishContainerPrepare(war, null, (Class<?>[]) null);
    }

    protected Client client;

    @Before
    public void beforeTest() {
        client = ClientBuilder.newClient();
    }

    @After
    public void afterTest() {
        client.close();
        client = null;
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ValidationWithCDITest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Tests Bean Validation constraints on method parameters
     * @tpSince RESTEasy 3.1.0
     */
    @Test
    public void testRoot() throws Exception {
        WebTarget base = client.target(generateURL("/test/root/sub?foo=x"));
        Builder builder = base.request();
        builder.accept(MediaType.APPLICATION_XML);
        Response response = builder.get();
        Assert.assertEquals(400, response.getStatus());
        Object header = response.getHeaders().getFirst(Validation.VALIDATION_HEADER);
        Assert.assertTrue(header instanceof String);
        Assert.assertTrue(Boolean.valueOf(String.class.cast(header)));
        ViolationReport report = response.readEntity(ViolationReport.class);
        countViolations(report, 0, 0, 1, 0);
    }

    /**
     * @tpTestDetails Tests Bean Validation constraints on method parameters
     * @tpSince RESTEasy 3.1.0
     */
    @Test
    public void testAsynch() throws Exception {
        WebTarget base = client.target(generateURL("/test/async/sub"));

        {
            Builder builder = base.queryParam("foo", "x").request();
            builder.accept(MediaType.APPLICATION_XML);
            Response response = builder.get();
            Assert.assertEquals(400, response.getStatus());
            Object header = response.getHeaders().getFirst(Validation.VALIDATION_HEADER);
            Assert.assertTrue(header instanceof String);
            Assert.assertTrue(Boolean.valueOf(String.class.cast(header)));
            ViolationReport report = response.readEntity(ViolationReport.class);
            countViolations(report, 0, 0, 1, 0);
            response.close();
        }

        {
            Builder builder = base.queryParam("foo", "xy").request();
            builder.accept(MediaType.APPLICATION_XML);
            Response response = builder.get();
            Assert.assertEquals(200, response.getStatus());
            response.close();
        }

        {
            Builder builder = base.queryParam("foo", "x").request();
            builder.accept(MediaType.APPLICATION_XML);
            Response response = builder.get();
            Assert.assertEquals(400, response.getStatus());
            Object header = response.getHeaders().getFirst(Validation.VALIDATION_HEADER);
            Assert.assertTrue(header instanceof String);
            Assert.assertTrue(Boolean.valueOf(String.class.cast(header)));
            ViolationReport report = response.readEntity(ViolationReport.class);
            countViolations(report, 0, 0, 1, 0);
            response.close();
        }
    }

    private void countViolations(ViolationReport e, int propertyCount, int classCount, int parameterCount,
            int returnValueCount) {
        Assert.assertEquals(propertyCount, e.getPropertyViolations().size());
        Assert.assertEquals(classCount, e.getClassViolations().size());
        Assert.assertEquals(parameterCount, e.getParameterViolations().size());
        Assert.assertEquals(returnValueCount, e.getReturnValueViolations().size());
    }
}
