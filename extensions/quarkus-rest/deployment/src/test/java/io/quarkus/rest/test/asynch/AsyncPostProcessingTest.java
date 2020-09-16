package io.quarkus.rest.test.asynch;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.asynch.resource.AsyncPostProcessingInterceptor;
import io.quarkus.rest.test.asynch.resource.AsyncPostProcessingMsgBodyWriterInterceptor;
import io.quarkus.rest.test.asynch.resource.AsyncPostProcessingResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Asynchronous RESTEasy
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-767
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Async Post Processing Test")
public class AsyncPostProcessingTest {

    private static Logger logger = Logger.getLogger(AsyncPostProcessingTest.class);

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(TestUtil.class, PortProviderUtil.class);
            // war.addAsWebInfResource(AsyncPostProcessingTest.class.getPackage(), "AsyncPostProcessingTestWeb.xml",
            // "web.xml");
            // Arquillian in the deployment
            return TestUtil.finishContainerPrepare(war, null, AsyncPostProcessingResource.class,
                    AsyncPostProcessingMsgBodyWriterInterceptor.class, AsyncPostProcessingInterceptor.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, AsyncPostProcessingTest.class.getSimpleName());
    }

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Test synchronized request.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Sync")
    public void testSync() throws Exception {
        reset();
        Response response = client.target(generateURL("/sync")).request().get();
        logger.info("Status: " + response.getStatus());
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        logger.info("TestMessageBodyWriterInterceptor.called: " + AsyncPostProcessingMsgBodyWriterInterceptor.called);
        logger.info("TestPostProcessInterceptor.called: " + AsyncPostProcessingInterceptor.called);
        response.bufferEntity();
        logger.info("returned entity: " + response.readEntity(String.class));
        Assertions.assertTrue(AsyncPostProcessingMsgBodyWriterInterceptor.called,
                "AsyncPostProcessingMsgBodyWriterInterceptor interceptor was not called");
        Assertions.assertTrue(AsyncPostProcessingInterceptor.called,
                "AsyncPostProcessingInterceptor interceptor was not called");
        Assertions.assertEquals("sync", response.readEntity(String.class), "Entity has wrong content");
    }

    /**
     * @tpTestDetails Test async request with delay.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Async With Delay")
    public void testAsyncWithDelay() throws Exception {
        reset();
        Response response = client.target(generateURL("/async/delay")).request().get();
        logger.info("Status: " + response.getStatus());
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        logger.info("TestMessageBodyWriterInterceptor.called: " + AsyncPostProcessingMsgBodyWriterInterceptor.called);
        logger.info("TestPostProcessInterceptor.called: " + AsyncPostProcessingInterceptor.called);
        response.bufferEntity();
        logger.info("returned entity: " + response.readEntity(String.class));
        Assertions.assertTrue(AsyncPostProcessingMsgBodyWriterInterceptor.called,
                "AsyncPostProcessingMsgBodyWriterInterceptor interceptor was not called");
        Assertions.assertTrue(AsyncPostProcessingInterceptor.called,
                "AsyncPostProcessingInterceptor interceptor was not called");
        Assertions.assertEquals("async/delay", response.readEntity(String.class), "Entity has wrong content");
    }

    /**
     * @tpTestDetails Test async request without delay.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Async With No Delay")
    public void testAsyncWithNoDelay() throws Exception {
        reset();
        Response response = client.target(generateURL("/async/nodelay")).request().get();
        logger.info("Status: " + response.getStatus());
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        logger.info("TestMessageBodyWriterInterceptor.called: " + AsyncPostProcessingMsgBodyWriterInterceptor.called);
        logger.info("TestPostProcessInterceptor.called: " + AsyncPostProcessingInterceptor.called);
        response.bufferEntity();
        logger.info("returned entity: " + response.readEntity(String.class));
        Assertions.assertTrue(AsyncPostProcessingMsgBodyWriterInterceptor.called,
                "AsyncPostProcessingMsgBodyWriterInterceptor interceptor was not called");
        Assertions.assertTrue(AsyncPostProcessingInterceptor.called,
                "AsyncPostProcessingInterceptor interceptor was not called");
        Assertions.assertEquals("async/nodelay", response.readEntity(String.class), "Entity has wrong content");
    }

    private void reset() {
        AsyncPostProcessingMsgBodyWriterInterceptor.called = false;
        AsyncPostProcessingInterceptor.called = false;
    }
}
