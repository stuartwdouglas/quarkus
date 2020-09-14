package io.quarkus.rest.test.interceptor;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
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

import io.quarkus.rest.test.interceptor.resource.PriorityExecutionClientRequestFilter1;
import io.quarkus.rest.test.interceptor.resource.PriorityExecutionClientRequestFilter2;
import io.quarkus.rest.test.interceptor.resource.PriorityExecutionClientRequestFilter3;
import io.quarkus.rest.test.interceptor.resource.PriorityExecutionClientRequestFilterMax;
import io.quarkus.rest.test.interceptor.resource.PriorityExecutionClientRequestFilterMin;
import io.quarkus.rest.test.interceptor.resource.PriorityExecutionClientResponseFilter1;
import io.quarkus.rest.test.interceptor.resource.PriorityExecutionClientResponseFilter2;
import io.quarkus.rest.test.interceptor.resource.PriorityExecutionClientResponseFilter3;
import io.quarkus.rest.test.interceptor.resource.PriorityExecutionClientResponseFilterMax;
import io.quarkus.rest.test.interceptor.resource.PriorityExecutionClientResponseFilterMin;
import io.quarkus.rest.test.interceptor.resource.PriorityExecutionContainerRequestFilter1;
import io.quarkus.rest.test.interceptor.resource.PriorityExecutionContainerRequestFilter2;
import io.quarkus.rest.test.interceptor.resource.PriorityExecutionContainerRequestFilter3;
import io.quarkus.rest.test.interceptor.resource.PriorityExecutionContainerRequestFilterMax;
import io.quarkus.rest.test.interceptor.resource.PriorityExecutionContainerRequestFilterMin;
import io.quarkus.rest.test.interceptor.resource.PriorityExecutionContainerResponseFilter1;
import io.quarkus.rest.test.interceptor.resource.PriorityExecutionContainerResponseFilter2;
import io.quarkus.rest.test.interceptor.resource.PriorityExecutionContainerResponseFilter3;
import io.quarkus.rest.test.interceptor.resource.PriorityExecutionContainerResponseFilterMax;
import io.quarkus.rest.test.interceptor.resource.PriorityExecutionContainerResponseFilterMin;
import io.quarkus.rest.test.interceptor.resource.PriorityExecutionResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Interceptors
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 * @tpTestCaseDetails Regression test for RESTEASY-1294
 */

public class PriorityExecutionTest {
    public static volatile Queue<String> interceptors = new ConcurrentLinkedQueue<String>();
    public static Logger logger = Logger.getLogger(PriorityExecutionTest.class);
    private static final String WRONG_ORDER_ERROR_MSG = "Wrong order of interceptor execution";

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(TestUtil.class, PortProviderUtil.class);
                    war.addClasses(PriorityExecutionClientResponseFilterMin.class,
                            PriorityExecutionClientResponseFilter1.class,
                            PriorityExecutionClientRequestFilter2.class,
                            PriorityExecutionClientRequestFilterMax.class,
                            PriorityExecutionClientRequestFilter1.class,
                            PriorityExecutionClientResponseFilter2.class,
                            PriorityExecutionClientRequestFilter3.class,
                            PriorityExecutionClientResponseFilter3.class,
                            PriorityExecutionClientResponseFilterMax.class,
                            PriorityExecutionClientRequestFilterMin.class);
                    // Arquillian in the deployment

                    // finish preparation of war container, define end-point and filters
                    return TestUtil.finishContainerPrepare(war, null,
                            // end-point
                            PriorityExecutionResource.class,
                            // server filters
                            PriorityExecutionContainerResponseFilter2.class,
                            PriorityExecutionContainerResponseFilter1.class,
                            PriorityExecutionContainerResponseFilter3.class,
                            PriorityExecutionContainerResponseFilterMin.class,
                            PriorityExecutionContainerResponseFilterMax.class,
                            PriorityExecutionContainerRequestFilter2.class,
                            PriorityExecutionContainerRequestFilter1.class,
                            PriorityExecutionContainerRequestFilter3.class,
                            PriorityExecutionContainerRequestFilterMin.class,
                            PriorityExecutionContainerRequestFilterMax.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, PriorityExecutionTest.class.getSimpleName());
    }

    static Client client;

    @Before
    public void setup() {
        client = ClientBuilder.newClient();
    }

    @After
    public void cleanup() {
        client.close();
    }

    /**
     * @tpTestDetails Check order of client and server filters
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testPriority() throws Exception {
        client.register(PriorityExecutionClientResponseFilter3.class);
        client.register(PriorityExecutionClientResponseFilter1.class);
        client.register(PriorityExecutionClientResponseFilter2.class);
        client.register(PriorityExecutionClientResponseFilterMin.class);
        client.register(PriorityExecutionClientResponseFilterMax.class);
        client.register(PriorityExecutionClientRequestFilter3.class);
        client.register(PriorityExecutionClientRequestFilter1.class);
        client.register(PriorityExecutionClientRequestFilter2.class);
        client.register(PriorityExecutionClientRequestFilterMin.class);
        client.register(PriorityExecutionClientRequestFilterMax.class);

        Response response = client.target(generateURL("/test")).request().get();
        response.bufferEntity();
        logger.info(response.readEntity(String.class));
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("Wrong content of response", "test", response.getEntity());

        // client filters
        Assert.assertEquals(WRONG_ORDER_ERROR_MSG, "PriorityExecutionClientRequestFilterMin", interceptors.poll());
        Assert.assertEquals(WRONG_ORDER_ERROR_MSG, "PriorityExecutionClientRequestFilter1", interceptors.poll());
        Assert.assertEquals(WRONG_ORDER_ERROR_MSG, "PriorityExecutionClientRequestFilter2", interceptors.poll());
        Assert.assertEquals(WRONG_ORDER_ERROR_MSG, "PriorityExecutionClientRequestFilter3", interceptors.poll());
        Assert.assertEquals(WRONG_ORDER_ERROR_MSG, "PriorityExecutionClientRequestFilterMax", interceptors.poll());

        // server filters
        Assert.assertEquals(WRONG_ORDER_ERROR_MSG, "PriorityExecutionContainerRequestFilterMin", interceptors.poll());
        Assert.assertEquals(WRONG_ORDER_ERROR_MSG, "PriorityExecutionContainerRequestFilter1", interceptors.poll());
        Assert.assertEquals(WRONG_ORDER_ERROR_MSG, "PriorityExecutionContainerRequestFilter2", interceptors.poll());
        Assert.assertEquals(WRONG_ORDER_ERROR_MSG, "PriorityExecutionContainerRequestFilter3", interceptors.poll());
        Assert.assertEquals(WRONG_ORDER_ERROR_MSG, "PriorityExecutionContainerRequestFilterMax", interceptors.poll());
        Assert.assertEquals(WRONG_ORDER_ERROR_MSG, "PriorityExecutionContainerResponseFilterMax", interceptors.poll());
        Assert.assertEquals(WRONG_ORDER_ERROR_MSG, "PriorityExecutionContainerResponseFilter3", interceptors.poll());
        Assert.assertEquals(WRONG_ORDER_ERROR_MSG, "PriorityExecutionContainerResponseFilter2", interceptors.poll());
        Assert.assertEquals(WRONG_ORDER_ERROR_MSG, "PriorityExecutionContainerResponseFilter1", interceptors.poll());
        Assert.assertEquals(WRONG_ORDER_ERROR_MSG, "PriorityExecutionContainerResponseFilterMin", interceptors.poll());

        // client filters
        Assert.assertEquals(WRONG_ORDER_ERROR_MSG, "PriorityExecutionClientResponseFilterMax", interceptors.poll());
        Assert.assertEquals(WRONG_ORDER_ERROR_MSG, "PriorityExecutionClientResponseFilter3", interceptors.poll());
        Assert.assertEquals(WRONG_ORDER_ERROR_MSG, "PriorityExecutionClientResponseFilter2", interceptors.poll());
        Assert.assertEquals(WRONG_ORDER_ERROR_MSG, "PriorityExecutionClientResponseFilter1", interceptors.poll());
        Assert.assertEquals(WRONG_ORDER_ERROR_MSG, "PriorityExecutionClientResponseFilterMin", interceptors.poll());
    }
}
