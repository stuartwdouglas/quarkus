package io.quarkus.rest.test.asynch;

import static org.jboss.resteasy.utils.PortProviderUtil.generateURL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestClientBuilder;
import io.quarkus.rest.test.asynch.resource.AsynchBasicResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;

/**
 * @tpSubChapter Asynchronous RESTEasy
 * @tpChapter Integration tests
 * @tpTestCaseDetails Basic asynchronous test for "resteasy.async.job.service.max.job.results" property.
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Asynch Basic Test")
public class AsynchBasicTest {

    private static org.jboss.logging.Logger logger = org.jboss.logging.Logger.getLogger(AsynchBasicTest.class);

    public static CountDownLatch latch;

    private static final String DEFAULT_DEPLOYMENT = "AsynchBasicTestBasic";

    private static final String ONE_MAX_DEPLOYMENT = "AsynchBasicTestOne";

    private static final String TEN_MAX_DEPLOYMENT = "AsynchBasicTestTen";

    public static Archive<?> deploy(String deploymentName, String maxSize) {
        WebArchive war = TestUtil.prepareArchive(deploymentName);
        war.addClass(PortProviderUtil.class);
        war.addClass(TestUtil.class);
        Map<String, String> contextParam = new HashMap<>();
        contextParam.put("resteasy.async.job.service.enabled", "true");
        if (maxSize != null) {
            contextParam.put("resteasy.async.job.service.max.job.results", maxSize);
        }
        // Arquillian in the deployment
        return TestUtil.finishContainerPrepare(war, contextParam, AsynchBasicResource.class);
    }

    @Deployment(name = DEFAULT_DEPLOYMENT)
    public static Archive<?> deployDefault() {
        return deploy(DEFAULT_DEPLOYMENT, null);
    }

    @Deployment(name = ONE_MAX_DEPLOYMENT)
    public static Archive<?> deployOne() {
        return deploy(ONE_MAX_DEPLOYMENT, "1");
    }

    @Deployment(name = TEN_MAX_DEPLOYMENT)
    public static Archive<?> deployTen() {
        return deploy(TEN_MAX_DEPLOYMENT, "10");
    }

    private QuarkusRestClient initClient() {
        return ((QuarkusRestClientBuilder) ClientBuilder.newBuilder()).readTimeout(5, TimeUnit.SECONDS)
                .connectionCheckoutTimeout(5, TimeUnit.SECONDS).connectTimeout(5, TimeUnit.SECONDS).build();
    }

    /**
     * @tpTestDetails Test oneway=true query parameter
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @OperateOnDeployment(DEFAULT_DEPLOYMENT)
    @DisplayName("Test Oneway")
    public void testOneway() throws Exception {
        QuarkusRestClient client = initClient();
        Response response = null;
        try {
            latch = new CountDownLatch(1);
            long start = System.currentTimeMillis();
            response = client.target(generateURL("?oneway=true", DEFAULT_DEPLOYMENT)).request()
                    .put(Entity.entity("content", "text/plain"));
            // response = request.put();
            long end = System.currentTimeMillis() - start;
            Assertions.assertEquals(Status.ACCEPTED.getStatusCode(), response.getStatus());
            Assertions.assertTrue(end < 1000);
            Assertions.assertTrue(latch.await(2, TimeUnit.SECONDS), "Request was not sent correctly");
        } finally {
            response.close();
            client.close();
        }
    }

    /**
     * @tpTestDetails Use default value of resteasy.async.job.service.max.job.results
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @OperateOnDeployment(DEFAULT_DEPLOYMENT)
    @DisplayName("Test Asynch Basic")
    public void testAsynchBasic() throws Exception {
        final int MAX = 4;
        QuarkusRestClient client = initClient();
        latch = new CountDownLatch(1);
        long start = System.currentTimeMillis();
        Response response = client.target(generateURL("?asynch=true", DEFAULT_DEPLOYMENT)).request()
                .post(Entity.entity("content", "text/plain"));
        @SuppressWarnings("unused")
        long end = System.currentTimeMillis() - start;
        Assertions.assertEquals(Status.ACCEPTED.getStatusCode(), response.getStatus());
        String jobUrl = response.getHeaderString(HttpHeaders.LOCATION);
        response.close();
        response = client.target(jobUrl).request().get();
        Assertions.assertTrue(latch.await(3, TimeUnit.SECONDS), "Request was not sent correctly");
        response.close();
        // there's a lag between when the latch completes and the executor
        // registers the completion of the call
        for (int i = 0; i <= MAX; i++) {
            response = client.target(jobUrl).request().get();
            Thread.sleep(1000);
            if (Status.OK == response.getStatus()) {
                Assertions.assertEquals("content", response.readEntity(String.class), "Wrong response content");
                response.close();
                break;
            }
            response.close();
            if (i == MAX) {
                Assertions.fail("Expected response with status code 200");
            }
        }
        // test its still there
        response = client.target(jobUrl).request().get();
        Thread.sleep(1000);
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("content", response.readEntity(String.class), "Wrong response content");
        response.close();
        // delete and test delete
        response = client.target(jobUrl).request().delete();
        Assertions.assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        response.close();
        response = client.target(jobUrl).request().get();
        Thread.sleep(1000);
        Assertions.assertEquals(Status.GONE.getStatusCode(), response.getStatus());
        response.close();
        client.close();
    }

    /**
     * @tpTestDetails Set value of resteasy.async.job.service.max.job.results to 1. Try to store to cache to items.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @OperateOnDeployment(ONE_MAX_DEPLOYMENT)
    @DisplayName("Test Asynch One")
    public void testAsynchOne() throws Exception {
        QuarkusRestClient client = initClient();
        // test cache size
        latch = new CountDownLatch(1);
        Response response = client.target(generateURL("?asynch=true", ONE_MAX_DEPLOYMENT)).request()
                .post(Entity.entity("content", "text/plain"));
        Assertions.assertEquals(Status.ACCEPTED.getStatusCode(), response.getStatus());
        String jobUrl1 = response.getHeaderString(HttpHeaders.LOCATION);
        Assertions.assertTrue(latch.await(3, TimeUnit.SECONDS), "Request was not sent correctly");
        response.close();
        latch = new CountDownLatch(1);
        response = client.target(generateURL("?asynch=true", ONE_MAX_DEPLOYMENT)).request()
                .post(Entity.entity("content", "text/plain"));
        Assertions.assertEquals(Status.ACCEPTED.getStatusCode(), response.getStatus());
        String jobUrl2 = response.getHeaderString(HttpHeaders.LOCATION);
        Assertions.assertTrue(latch.await(3, TimeUnit.SECONDS), "Request was not sent correctly");
        Assertions.assertTrue(!jobUrl1.equals(jobUrl2), "There are only one response for two requests");
        response.close();
        response = client.target(jobUrl1).request().get();
        Thread.sleep(1000);
        Assertions.assertEquals(Status.GONE.getStatusCode(), response.getStatus(),
                "Response should be gone, but server still remember it");
        response.close();
        // test its still there
        response = client.target(jobUrl2).request().get();
        Thread.sleep(1000);
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("content", response.readEntity(String.class), "Wrong content of response");
        response.close();
        // delete and test delete
        response = client.target(jobUrl2).request().delete();
        Assertions.assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        response.close();
        response = client.target(jobUrl2).request().get();
        Assertions.assertEquals(Status.GONE.getStatusCode(), response.getStatus());
        response.close();
        client.close();
    }

    /**
     * @tpTestDetails Test default value of resteasy.server.cache.maxsize. It should be 100. Try to store 110 items to cache. 10
     *                items should be gone.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @OperateOnDeployment(DEFAULT_DEPLOYMENT)
    @DisplayName("Test Asynch Max Size Default")
    public void testAsynchMaxSizeDefault() throws Exception {
        QuarkusRestClient client = initClient();
        ArrayList<String> jobs = new ArrayList<>();
        for (int i = 0; i < 110; i++) {
            // test cache size
            latch = new CountDownLatch(1);
            Response response = client.target(generateURL("?asynch=true", DEFAULT_DEPLOYMENT)).request()
                    .post(Entity.entity("content", "text/plain"));
            Assertions.assertEquals(Status.ACCEPTED.getStatusCode(), response.getStatus());
            String jobUrl = response.getHeaderString(HttpHeaders.LOCATION);
            logger.info(i + ": " + jobUrl);
            jobs.add(jobUrl);
            response.close();
            Thread.sleep(50);
        }
        Thread.sleep(2000);
        for (int i = 0; i < 10; i++) {
            Response response = client.target(jobs.get(i)).request().get();
            logger.info(i + " (" + jobs.get(i) + "): get " + response.getStatus() + ", expected: " + Status.GONE);
            Assertions.assertEquals(Status.GONE.getStatusCode(), response.getStatus(),
                    "Response should be gone, but server still remember it");
            response.close();
            Thread.sleep(50);
        }
        for (int i = 10; i < 110; i++) {
            Response response = client.target(jobs.get(i)).request().get();
            logger.info(i + " (" + jobs.get(i) + "): get " + response.getStatus() + ", expected: " + Status.OK);
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals("content", response.readEntity(String.class), "Wrong content of response");
            response.close();
            Thread.sleep(50);
        }
        client.close();
    }

    /**
     * @tpTestDetails Set value of resteasy.server.cache.maxsize to 10. Try to restore item from cache.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @OperateOnDeployment(TEN_MAX_DEPLOYMENT)
    @DisplayName("Test Asynch Ten")
    public void testAsynchTen() throws Exception {
        QuarkusRestClient client = initClient();
        // test readAndRemove
        latch = new CountDownLatch(1);
        Response response = client.target(generateURL("?asynch=true", TEN_MAX_DEPLOYMENT)).request()
                .post(Entity.entity("content", "text/plain"));
        Assertions.assertEquals(Status.ACCEPTED.getStatusCode(), response.getStatus());
        String jobUrl2 = response.getHeaderString(HttpHeaders.LOCATION);
        Assertions.assertTrue(latch.await(3, TimeUnit.SECONDS), "Request was not sent correctly");
        response.close();
        Thread.sleep(50);
        // test its still there
        response = client.target(jobUrl2).request().post(Entity.text(new String()));
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("content", response.readEntity(String.class), "Wrong content of response");
        response.close();
        Thread.sleep(50);
        response = client.target(jobUrl2).request().get();
        Thread.sleep(1000);
        Assertions.assertEquals(Status.GONE.getStatusCode(), response.getStatus());
        response.close();
        client.close();
    }
}
