package io.quarkus.rest.test.resource.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.resource.basic.resource.ResourceInfoInjectionFilter;
import io.quarkus.rest.test.resource.basic.resource.ResourceInfoInjectionResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resources
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for JBEAP-4701
 * @tpSince RESTEasy 3.0.17
 */
@DisplayName("Resource Info Injection Test")
public class ResourceInfoInjectionTest {

    protected static Client client;

    @BeforeAll
    public static void init() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void close() {
        client.close();
    }

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ResourceInfoInjectionTest.class.getSimpleName());
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, ResourceInfoInjectionFilter.class,
                    ResourceInfoInjectionResource.class);
        }
    });

    /**
     * @tpTestDetails Check for injecting ResourceInfo object in ContainerResponseFilter
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    @DisplayName("Test Not Found")
    public void testNotFound() throws Exception {
        WebTarget target = client.target(generateURL("/bogus"));
        Response response = target.request().get();
        String entity = response.readEntity(String.class);
        Assertions.assertEquals(Status.NOT_FOUND.getStatusCode() * 2, response.getStatus(),
                "ResponseFilter was probably not applied to response");
        Assertions.assertTrue(entity.contains("RESTEASY003210"), "Wrong body of response");
    }

    /**
     * @tpTestDetails Check for injecting ResourceInfo object in end-point
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    @DisplayName("Test Async")
    public void testAsync() throws Exception {
        WebTarget target = client.target(generateURL("/async"));
        Response response = target.request().post(Entity.entity("hello", "text/plain"));
        String val = response.readEntity(String.class);
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus(), "OK status is expected");
        Assertions.assertEquals("async", val, "Wrong body of response");
    }
}
