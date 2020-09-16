package io.quarkus.rest.test.asynch;

import java.util.function.Supplier;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
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
import io.quarkus.rest.test.asynch.resource.JaxrsAsyncServletApp;
import io.quarkus.rest.test.asynch.resource.JaxrsAsyncServletAsyncResponseBlockingQueue;
import io.quarkus.rest.test.asynch.resource.JaxrsAsyncServletJaxrsResource;
import io.quarkus.rest.test.asynch.resource.JaxrsAsyncServletPrintingErrorHandler;
import io.quarkus.rest.test.asynch.resource.JaxrsAsyncServletResource;
import io.quarkus.rest.test.asynch.resource.JaxrsAsyncServletServiceUnavailableExceptionMapper;
import io.quarkus.rest.test.asynch.resource.JaxrsAsyncServletTimeoutHandler;
import io.quarkus.rest.test.asynch.resource.JaxrsAsyncServletXmlData;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.util.TimeoutUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Asynchronous RESTEasy
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for asyncHttpServlet module. Check cooperation during more requests and exception mapping.
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Jaxrs Async Servlet Test")
public class JaxrsAsyncServletTest {

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
            war.addClasses(JaxrsAsyncServletXmlData.class, JaxrsAsyncServletAsyncResponseBlockingQueue.class,
                    JaxrsAsyncServletJaxrsResource.class, JaxrsAsyncServletApp.class, JaxrsAsyncServletTimeoutHandler.class,
                    JaxrsAsyncServletResource.class, JaxrsAsyncServletPrintingErrorHandler.class,
                    JaxrsAsyncServletServiceUnavailableExceptionMapper.class, JaxrsAsyncServletXmlData.class);
            // war.addAsWebInfResource(AsyncPostProcessingTest.class.getPackage(), "JaxrsAsyncServletWeb.xml", "web.xml");
            return war;
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, AsyncServletTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Check ForbiddenException throwing during async request. Try to inject un-exist bean.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Injection Failure")
    public void testInjectionFailure() throws Exception {
        long start = System.currentTimeMillis();
        Client client = ClientBuilder.newClient();
        Response response = client.target(generateURL("/jaxrs/injection-failure/abcd")).request().get();
        Assertions.assertEquals(Response.Status.NOT_ACCEPTABLE.getStatusCode(), response.getStatus());
        Assertions.assertTrue(response.readEntity(String.class).contains(NotFoundException.class.getName()),
                "ForbiddenException was not thrown");
        long end = System.currentTimeMillis() - start;
        // should take less than 1 second
        Assertions.assertTrue(end < 1000, "Wrong time of request");
        response.close();
        client.close();
    }

    /**
     * @tpTestDetails Check ForbiddenException throwing during async request. No injection of some beans.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Method Failure")
    public void testMethodFailure() throws Exception {
        long start = System.currentTimeMillis();
        Client client = ClientBuilder.newClient();
        Response response = client.target(generateURL("/jaxrs/method-failure")).request().get();
        Assertions.assertEquals(Response.Status.NOT_ACCEPTABLE.getStatusCode(), response.getStatus());
        Assertions.assertTrue(response.readEntity(String.class).contains(ForbiddenException.class.getName()),
                "ForbiddenException was not thrown");
        long end = System.currentTimeMillis() - start;
        // should take less than 1 second
        Assertions.assertTrue(end < 1000, "Wrong time of request");
        response.close();
        client.close();
    }

    /**
     * @tpTestDetails Try to get xml response.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Async")
    public void testAsync() throws Exception {
        Client client = ClientBuilder.newClient();
        long start = System.currentTimeMillis();
        Response response = client.target(generateURL("/jaxrs")).request().get();
        long end = System.currentTimeMillis() - start;
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("hello", response.readEntity(String.class), "Wrong content of response");
        // The time out is set to 2 seconds, this is a best guess test and if future failures are present this should be
        // reconsidered with some sort of offset.
        Assertions.assertTrue(end < 2000, "Wrong time of request");
        response.close();
        client.close();
    }

    /**
     * @tpTestDetails Check timeout exception
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Timeout")
    public void testTimeout() throws Exception {
        Client client = ClientBuilder.newClient();
        Response response = client.target(generateURL("/jaxrs/timeout")).request().get();
        // exception mapper from another test overrides 503 to 408
        Assertions.assertEquals(Status.REQUEST_TIMEOUT.getStatusCode(), response.getStatus());
        response.close();
        client.close();
    }

    /**
     * @tpTestDetails Test cooperation between two requests. Use 408 HTTP status.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Cancel")
    public void testCancel() throws Exception {
        Client client = ClientBuilder.newClient();
        Response response = client.target(generateURL("/jaxrs/cancel")).request().get();
        Assertions.assertEquals(Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
        response.close();
        // It is possible, that thread created in JaxrsAsyncServletJaxrsResource.cancel method
        // don't finish before next request is called. We need to wait some time and do this request again.
        // Default timeout is 20s
        boolean ok = false;
        for (int i = 0; i < TimeoutUtil.adjust(20); i++) {
            response = client.target(generateURL("/jaxrs/cancelled")).request().get();
            int status = response.getStatus();
            response.close();
            if (status == Status.NO_CONTENT.getStatusCode()) {
                ok = true;
                break;
            }
            Thread.sleep(1000);
        }
        Assertions.assertTrue(ok, "Response was not canceled correctly");
        client.close();
    }

    /**
     * @tpTestDetails Test cooperation between two requests. Use 200 HTTP status.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Resume Object")
    public void testResumeObject() throws Exception {
        Client client = ClientBuilder.newClient();
        // client.register(JAXBXmlRootElementProvider.class);
        long start = System.currentTimeMillis();
        Response response = client.target(generateURL("/jaxrs/resume/object")).request().get();
        long end = System.currentTimeMillis() - start;
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("bill", response.readEntity(JaxrsAsyncServletXmlData.class).getName(),
                "Wrong content of response");
        Assertions.assertTrue(end < 1500, "Wrong time of request");
        response.close();
        client.close();
    }

    /**
     * @tpTestDetails Create response in new thread.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Resume Object Thread")
    public void testResumeObjectThread() throws Exception {
        Client client = ClientBuilder.newClient();
        // client.register(JAXBXmlRootElementProvider.class);
        long start = System.currentTimeMillis();
        Response response = client.target(generateURL("/jaxrs/resume/object/thread")).request().get();
        long end = System.currentTimeMillis() - start;
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("bill", response.readEntity(JaxrsAsyncServletXmlData.class).getName(),
                "Wrong content of response");
        // should take less than 1 second
        Assertions.assertTrue(end < 1000, "Wrong time of request");
        response.close();
        client.close();
    }
}
