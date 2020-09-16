package io.quarkus.rest.test.asynch;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.asynch.resource.AsyncServletResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Asynchronous RESTEasy
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for asyncHttpServlet module
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Async Servlet Test")
public class AsyncServletTest {

    static QuarkusRestClient client;

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, AsyncServletResource.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, AsyncServletTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test for correct response
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Async")
    public void testAsync() throws Exception {
        Response response = client.target(generateURL("/async")).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("hello", response.readEntity(String.class), "Wrong response content");
    }

    /**
     * @tpTestDetails Service unavailable test
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Timeout")
    public void testTimeout() throws Exception {
        Response response = client.target(generateURL("/async/timeout")).request().get();
        Assertions.assertEquals(Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
    }
}
