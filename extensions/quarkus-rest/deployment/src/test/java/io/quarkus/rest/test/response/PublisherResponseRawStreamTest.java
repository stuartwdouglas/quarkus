package io.quarkus.rest.test.response;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.response.resource.AsyncResponseCallback;
import io.quarkus.rest.test.response.resource.AsyncResponseException;
import io.quarkus.rest.test.response.resource.AsyncResponseExceptionMapper;
import io.quarkus.rest.test.response.resource.PublisherResponseRawStreamResource;
import io.quarkus.rest.test.response.resource.SlowString;
import io.quarkus.rest.test.response.resource.SlowStringWriter;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Publisher response type
 * @tpChapter Integration tests
 * @tpSince RESTEasy 4.0
 */
@DisplayName("Publisher Response Raw Stream Test")
public class PublisherResponseRawStreamTest {

    Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.setManifest(new StringAsset("Manifest-Version: 1.0\n"
                    + "Dependencies: org.jboss.resteasy.resteasy-rxjava2 services, org.reactivestreams\n"));
            return TestUtil.finishContainerPrepare(war, null, PublisherResponseRawStreamResource.class, SlowStringWriter.class,
                    SlowString.class, AsyncResponseCallback.class, AsyncResponseExceptionMapper.class,
                    AsyncResponseException.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, PublisherResponseRawStreamTest.class.getSimpleName());
    }

    @BeforeEach
    public void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterEach
    public void close() {
        client.close();
        client = null;
    }

    /**
     * @tpTestDetails Resource method returns Publisher<String>.
     * @tpSince RESTEasy 4.0
     */
    @Test
    @DisplayName("Test Chunked")
    public void testChunked() throws Exception {
        Invocation.Builder request = client.target(generateURL("/chunked")).request();
        Response response = request.get();
        String entity = response.readEntity(String.class);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertTrue(entity.startsWith("0-11-12-1"));
        Assertions.assertTrue(entity.endsWith("29-1"));
    }

    /**
     * @tpTestDetails Resource method unsubscribes on close for infinite streams.
     * @tpSince RESTEasy 4.0
     */
    @Test
    @DisplayName("Test Infinite Streams Chunked")
    public void testInfiniteStreamsChunked() throws Exception {
        Invocation.Builder request = client.target(generateURL("/chunked-infinite")).request();
        Future<Response> futureResponse = request.async().get();
        try {
            futureResponse.get(2, TimeUnit.SECONDS);
        } catch (TimeoutException x) {
        }
        close();
        setup();
        Thread.sleep(5000);
        request = client.target(generateURL("/infinite-done")).request();
        Response response = request.get();
        String entity = response.readEntity(String.class);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(entity, "true");
    }

    @Test
    @DisplayName("Test Slow Async Writer")
    public void testSlowAsyncWriter() throws Exception {
        Invocation.Builder request = client.target(generateURL("/slow-async-io")).request();
        Response response = request.get();
        String entity = response.readEntity(String.class);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(entity, "onetwo");
    }
}
