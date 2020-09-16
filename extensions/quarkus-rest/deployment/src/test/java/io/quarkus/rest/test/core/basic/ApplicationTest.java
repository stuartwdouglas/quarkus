package io.quarkus.rest.test.core.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.core.basic.resource.ApplicationTestAExplicitApplication;
import io.quarkus.rest.test.core.basic.resource.ApplicationTestBExplicitApplication;
import io.quarkus.rest.test.core.basic.resource.ApplicationTestIgnoredApplication;
import io.quarkus.rest.test.core.basic.resource.ApplicationTestMappedApplication;
import io.quarkus.rest.test.core.basic.resource.ApplicationTestResourceA;
import io.quarkus.rest.test.core.basic.resource.ApplicationTestResourceB;
import io.quarkus.rest.test.core.basic.resource.ApplicationTestScannedApplication;
import io.quarkus.rest.test.core.basic.resource.ApplicationTestSingletonA;
import io.quarkus.rest.test.core.basic.resource.ApplicationTestSingletonB;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Configuration
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for usage of more application in one deployment
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Application Test")
public class ApplicationTest {

    private static final String CONTENT_ERROR_MESSAGE = "Wrong content of response";

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            // war.addAsWebInfResource(ApplicationTest.class.getPackage(), "ApplicationWeb.xml", "web.xml");
            war.addClasses(ApplicationTestAExplicitApplication.class, ApplicationTestBExplicitApplication.class,
                    ApplicationTestIgnoredApplication.class, ApplicationTestMappedApplication.class,
                    ApplicationTestResourceA.class, ApplicationTestResourceB.class, ApplicationTestSingletonA.class,
                    ApplicationTestSingletonB.class, ApplicationTestScannedApplication.class);
            return war;
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ApplicationTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test first application in deployment
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Explicit A")
    public void testExplicitA() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget base = client.target(generateURL("/a/explicit"));
        String value = base.path("resources/a").request().get(String.class);
        Assertions.assertEquals(CONTENT_ERROR_MESSAGE, "a", value);
        Response response = base.path("resources/b").request().get();
        Assertions.assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        value = base.path("singletons/a").request().get(String.class);
        Assertions.assertEquals(CONTENT_ERROR_MESSAGE, "a", value);
        response = base.path("singletons/b").request().get();
        Assertions.assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        client.close();
    }

    /**
     * @tpTestDetails Test second application in deployment
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Explicit B")
    public void testExplicitB() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget base = client.target(generateURL("/b/explicit"));
        String value = base.path("resources/b").request().get(String.class);
        Assertions.assertEquals(CONTENT_ERROR_MESSAGE, "b", value);
        Response response = base.path("resources/a").request().get();
        Assertions.assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        value = base.path("singletons/b").request().get(String.class);
        Assertions.assertEquals(CONTENT_ERROR_MESSAGE, "b", value);
        response = base.path("singletons/a").request().get();
        Assertions.assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        client.close();
    }

    /**
     * @tpTestDetails Test scanned application in deployment: getClasses and getSingletons methods are not used.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Scanned")
    public void testScanned() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget base = client.target(generateURL("/scanned"));
        String value = base.path("resources/a").request().get(String.class);
        Assertions.assertEquals(CONTENT_ERROR_MESSAGE, "a", value);
        value = base.path("resources/b").request().get(String.class);
        Assertions.assertEquals(CONTENT_ERROR_MESSAGE, "b", value);
        value = base.path("singletons/a").request().get(String.class);
        Assertions.assertEquals(CONTENT_ERROR_MESSAGE, "a", value);
        value = base.path("singletons/b").request().get(String.class);
        Assertions.assertEquals(CONTENT_ERROR_MESSAGE, "b", value);
        client.close();
    }

    /**
     * @tpTestDetails Test scanned application in deployment: getClasses and getSingletons methods are not used. This
     *                application is mapped to different location.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Mapped")
    public void testMapped() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget base = client.target(generateURL("/mapped"));
        String value = base.path("resources/a").request().get(String.class);
        Assertions.assertEquals(CONTENT_ERROR_MESSAGE, "a", value);
        value = base.path("resources/b").request().get(String.class);
        Assertions.assertEquals(CONTENT_ERROR_MESSAGE, "b", value);
        value = base.path("singletons/a").request().get(String.class);
        Assertions.assertEquals(CONTENT_ERROR_MESSAGE, "a", value);
        value = base.path("singletons/b").request().get(String.class);
        Assertions.assertEquals(CONTENT_ERROR_MESSAGE, "b", value);
        client.close();
    }
}
