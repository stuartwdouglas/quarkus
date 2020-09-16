package io.quarkus.rest.test.client;

import static org.junit.Assert.fail;

import java.util.function.Supplier;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestClientBuilder;
import io.quarkus.rest.test.client.resource.ClientExecutorShutdownTestResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpSince RESTEasy 3.0.16
 * @tpTestCaseDetails https://issues.jboss.org/browse/RESTEASY-621
 */
public class ClientExecutorShutdownTest extends ClientTestBase {
    private static Logger log = Logger.getLogger(ClientExecutorShutdownTest.class);

    @Path("/test")
    public interface TestService {
        @POST
        Response post();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(ClientExecutorShutdownTest.class);
                    war.addClass(ClientTestBase.class);
                    return TestUtil.finishContainerPrepare(war, null, ClientExecutorShutdownTestResource.class);
                }
            });

    /**
     * @tpTestDetails Verify that if ApacheHttpClient4Executor creates its own HttpClient,
     *                then ApacheHttpClient4Executor.finalize() will close the HttpClient's
     *                org.apache.http.conn.ClientConnectionManager.
     * @tpPassCrit ApacheHttpClient4Executor.finalize() will close the connection
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testApacheHttpClient4ExecutorNonSharedHttpClientFinalize() throws Throwable {
        ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine();
        QuarkusRestClient client = ((QuarkusRestClientBuilder) ClientBuilder.newBuilder()).httpEngine(engine).build();
        Response response = client.target(generateURL("/test")).request().post(null);
        Assert.assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        engine.finalize();
        HttpClient httpClient = engine.getHttpClient();
        HttpPost post = new HttpPost(generateURL("/test"));
        try {
            httpClient.execute(post);
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            log.info("Got expected IllegalStateException");
        }
    }

    /**
     * @tpTestDetails Verify that if ApacheHttpClient4Executor creates its own HttpClient,
     *                then ApacheHttpClient4Executor.close() will close the HttpClient's
     *                org.apache.http.conn.ClientConnectionManager.
     * @tpPassCrit ApacheHttpClient4Executor.close() will close the connection
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testApacheHttpClient4ExecutorNonSharedHttpClientClose() throws Throwable {
        ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine();
        QuarkusRestClient client = ((QuarkusRestClientBuilder) ClientBuilder.newBuilder()).httpEngine(engine).build();
        Response response = client.target(generateURL("/test")).request().post(null);
        Assert.assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        engine.close();
        HttpClient httpClient = engine.getHttpClient();
        HttpPost post = new HttpPost(generateURL("/test"));
        try {
            httpClient.execute(post);
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            log.info("Got expected IllegalStateException");
        }
    }

    /**
     * @tpTestDetails Verify that if ApacheHttpClient4Executor receives an HttpClient through
     *                a constructor, then ApacheHttpClient4Executor.finalize() will not close the
     *                HttpClient's org.apache.http.conn.ClientConnectionManager.
     * @tpPassCrit ApacheHttpClient4Executor.finalize() will not close the connection
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testApacheHttpClient4ExecutorSharedHttpClientFinalize() throws Throwable {
        HttpClient httpClient = HttpClientBuilder.create().build();
        ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine(httpClient, false);
        QuarkusRestClient client = ((QuarkusRestClientBuilder) ClientBuilder.newBuilder()).httpEngine(engine).build();
        Response response = client.target(generateURL("/test")).request().post(null);
        Assert.assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        engine.finalize();
        Assert.assertEquals("Original httpclient and engine httpclient are not the same instance",
                httpClient, engine.getHttpClient());
        HttpPost post = new HttpPost(generateURL("/test"));
        HttpResponse httpResponse = httpClient.execute(post);
        Assert.assertEquals("The httpclient was closed and it shouldn't", Status.NO_CONTENT.getStatusCode(),
                httpResponse.getStatusLine().getStatusCode());
    }

    /**
     * @tpTestDetails Verify that if ApacheHttpClient4Executor receives an HttpClient through
     *                a constructor, then ApacheHttpClient4Executor.close() will not close the
     *                HttpClient's org.apache.http.conn.ClientConnectionManager.
     * @tpPassCrit ApacheHttpClient4Executor.close() will not close the connection
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testApacheHttpClient4ExecutorSharedHttpClientClose() throws Throwable {
        HttpClient httpClient = HttpClientBuilder.create().build();
        ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine(httpClient, false);
        QuarkusRestClient client = ((QuarkusRestClientBuilder) ClientBuilder.newBuilder()).httpEngine(engine).build();
        Response response = client.target(generateURL("/test")).request().post(null);
        Assert.assertEquals("Original httpclient and engine httpclient are not the same instance",
                Status.NO_CONTENT, response.getStatus());
        engine.close();
        Assert.assertEquals(httpClient, engine.getHttpClient());
        HttpPost post = new HttpPost(generateURL("/test"));
        HttpResponse httpResponse = httpClient.execute(post);
        Assert.assertEquals("The httpclient was closed and it shouldn't", Status.NO_CONTENT.getStatusCode(),
                httpResponse.getStatusLine().getStatusCode());
    }
}
