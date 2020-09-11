package io.quarkus.rest.test.client;

import java.io.File;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocationBuilder;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestClientBuilder;
import io.quarkus.rest.test.client.resource.ChunkedTransferEncodingResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test facility for sending requests in chunked format
 * @tpSince RESTEasy 3.0.24
 */
public class ChunkedTransferEncodingTest {

    static QuarkusRestClient clientDefault;
    static QuarkusRestClient clientEngine43;
    static final String testFilePath;
    static long fileLength;
    static File file;

    static {
        testFilePath = TestUtil.getResourcePath(ChunkedTransferEncodingTest.class, "ChunkedTransferEncodingTestFile");
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, ChunkedTransferEncodingResource.class);
                }
            });

    @Before
    public void init() {
        file = new File(testFilePath);
        fileLength = file.length();
        clientDefault = (QuarkusRestClient) ClientBuilder.newClient();
        clientEngine43 = ((QuarkusRestClientBuilder) ClientBuilder.newBuilder()).httpEngine(new ApacheHttpClient43Engine())
                .build();
    }

    @After
    public void after() throws Exception {
        clientDefault.close();
        clientEngine43.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ChunkedTransferEncodingTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Tests that chunked Transfer-encoding header is set on ResteasyWebTarget. Tests that Content-Length
     *                header is set only in case when chunked transfer encoding is set to false. Headers are tested with the
     *                default client,
     *                client with te underlying http engines ApacheHttpClient4Engine and ApacheHttpClient43Engine.
     * @tpSince RESTEasy 3.0.24
     */
    @Test
    public void testTarget() throws Exception {
        doTestTarget(clientDefault, Boolean.TRUE, "chunked null");
        doTestTarget(clientDefault, Boolean.FALSE, "null " + fileLength);
        doTestTarget(clientDefault, null, "null " + fileLength);
        doTestTarget(clientEngine43, Boolean.TRUE, "chunked null");
        doTestTarget(clientEngine43, Boolean.FALSE, "null " + fileLength);
        doTestTarget(clientEngine43, null, "null " + fileLength);
    }

    public void doTestTarget(QuarkusRestClient client, Boolean b, String expected) throws Exception {
        ResteasyWebTarget target = client.target(generateURL("/test"));
        if (b == Boolean.TRUE || b == Boolean.FALSE) {
            target.setChunked(b.booleanValue());
        }
        Invocation.Builder request = target.request();
        Response response = request.post(Entity.entity(file, "text/plain"));
        String header = response.readEntity(String.class);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(expected, header);
    }

    /**
     * @tpTestDetails Tests that chunked Transfer-encoding header is set on ClientInvocationBuilder. Tests that Content-Length
     *                header is set only in case when chunked transfer encoding is set to false. Headers are tested with the
     *                default client,
     *                client with te underlying http engines ApacheHttpClient4Engine and ApacheHttpClient43Engine.
     * @tpSince RESTEasy 3.0.24
     */
    @Test
    public void testRequest() throws Exception {
        doTestRequest(clientDefault, Boolean.TRUE, "chunked null");
        doTestRequest(clientDefault, Boolean.FALSE, "null " + fileLength);
        doTestRequest(clientDefault, null, "null " + fileLength);
        doTestRequest(clientEngine43, Boolean.TRUE, "chunked null");
        doTestRequest(clientEngine43, Boolean.FALSE, "null " + fileLength);
        doTestRequest(clientEngine43, null, "null " + fileLength);
    }

    protected void doTestRequest(QuarkusRestClient client, Boolean b, String expected) throws Exception {
        ResteasyWebTarget target = client.target(generateURL("/test"));
        ClientInvocationBuilder request = (ClientInvocationBuilder) target.request();
        if (b != null) {
            request.setChunked(b);
        }
        Response response = request.post(Entity.entity(file, "text/plain"));
        String header = response.readEntity(String.class);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(expected, header);
    }
}
