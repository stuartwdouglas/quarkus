package io.quarkus.rest.test.asynch;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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

public class AsyncPostProcessingTest {

    private static Logger logger = Logger.getLogger(AsyncPostProcessingTest.class);
    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(TestUtil.class, PortProviderUtil.class);
                    war.addAsWebInfResource(AsyncPostProcessingTest.class.getPackage(), "AsyncPostProcessingTestWeb.xml",
                            "web.xml");
                    // Arquillian in the deployment

                    return TestUtil.finishContainerPrepare(war, null, AsyncPostProcessingResource.class,
                            AsyncPostProcessingMsgBodyWriterInterceptor.class, AsyncPostProcessingInterceptor.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, AsyncPostProcessingTest.class.getSimpleName());
    }

    @Before
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Test synchronized request.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testSync() throws Exception {
        reset();
        Response response = client.target(generateURL("/sync")).request().get();
        logger.info("Status: " + response.getStatus());
        Assert.assertEquals(Status.OK, response.getStatus());
        logger.info("TestMessageBodyWriterInterceptor.called: " + AsyncPostProcessingMsgBodyWriterInterceptor.called);
        logger.info("TestPostProcessInterceptor.called: " + AsyncPostProcessingInterceptor.called);
        response.bufferEntity();
        logger.info("returned entity: " + response.readEntity(String.class));
        Assert.assertTrue("AsyncPostProcessingMsgBodyWriterInterceptor interceptor was not called",
                AsyncPostProcessingMsgBodyWriterInterceptor.called);
        Assert.assertTrue("AsyncPostProcessingInterceptor interceptor was not called", AsyncPostProcessingInterceptor.called);
        Assert.assertEquals("Entity has wrong content", "sync", response.readEntity(String.class));
    }

    /**
     * @tpTestDetails Test async request with delay.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testAsyncWithDelay() throws Exception {
        reset();
        Response response = client.target(generateURL("/async/delay")).request().get();
        logger.info("Status: " + response.getStatus());
        Assert.assertEquals(Status.OK, response.getStatus());
        logger.info("TestMessageBodyWriterInterceptor.called: " + AsyncPostProcessingMsgBodyWriterInterceptor.called);
        logger.info("TestPostProcessInterceptor.called: " + AsyncPostProcessingInterceptor.called);
        response.bufferEntity();
        logger.info("returned entity: " + response.readEntity(String.class));
        Assert.assertTrue("AsyncPostProcessingMsgBodyWriterInterceptor interceptor was not called",
                AsyncPostProcessingMsgBodyWriterInterceptor.called);
        Assert.assertTrue("AsyncPostProcessingInterceptor interceptor was not called", AsyncPostProcessingInterceptor.called);
        Assert.assertEquals("Entity has wrong content", "async/delay", response.readEntity(String.class));
    }

    /**
     * @tpTestDetails Test async request without delay.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testAsyncWithNoDelay() throws Exception {
        reset();
        Response response = client.target(generateURL("/async/nodelay")).request().get();
        logger.info("Status: " + response.getStatus());
        Assert.assertEquals(Status.OK, response.getStatus());
        logger.info("TestMessageBodyWriterInterceptor.called: " + AsyncPostProcessingMsgBodyWriterInterceptor.called);
        logger.info("TestPostProcessInterceptor.called: " + AsyncPostProcessingInterceptor.called);
        response.bufferEntity();
        logger.info("returned entity: " + response.readEntity(String.class));
        Assert.assertTrue("AsyncPostProcessingMsgBodyWriterInterceptor interceptor was not called",
                AsyncPostProcessingMsgBodyWriterInterceptor.called);
        Assert.assertTrue("AsyncPostProcessingInterceptor interceptor was not called", AsyncPostProcessingInterceptor.called);
        Assert.assertEquals("Entity has wrong content", "async/nodelay", response.readEntity(String.class));
    }

    private void reset() {
        AsyncPostProcessingMsgBodyWriterInterceptor.called = false;
        AsyncPostProcessingInterceptor.called = false;
    }
}
