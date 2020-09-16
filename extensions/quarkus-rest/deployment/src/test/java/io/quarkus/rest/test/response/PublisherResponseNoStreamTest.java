package io.quarkus.rest.test.response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.SseEventSource;

import org.jboss.logging.Logger;
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
import io.quarkus.rest.test.response.resource.PublisherResponseNoStreamResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Publisher response type
 * @tpChapter Integration tests
 * @tpSince RESTEasy 4.0
 */
@DisplayName("Publisher Response No Stream Test")
public class PublisherResponseNoStreamTest {

    Client client;

    private static final Logger logger = Logger.getLogger(PublisherResponseNoStreamTest.class);

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.setManifest(new StringAsset("Manifest-Version: 1.0\n"
                    + "Dependencies: org.jboss.resteasy.resteasy-rxjava2 services, org.reactivestreams\n"));
            return TestUtil.finishContainerPrepare(war, null, PublisherResponseNoStreamResource.class,
                    AsyncResponseCallback.class, AsyncResponseExceptionMapper.class, AsyncResponseException.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, PublisherResponseNoStreamTest.class.getSimpleName());
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
    @DisplayName("Test Text")
    public void testText() throws Exception {
        Invocation.Builder request = client.target(generateURL("/text")).request();
        Response response = request.get();
        String entity = response.readEntity(String.class);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertTrue(entity.startsWith("[\"0-1\",\"1-1\""));
        Assertions.assertTrue(entity.endsWith(",\"29-1\"]"));
        // make sure the completion callback was called with no error
        request = client.target(generateURL("/callback-called-no-error/text")).request();
        response = request.get();
        Assertions.assertEquals(200, response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Resource method returns Publisher<String>, throws exception immediately.
     * @tpSince RESTEasy 4.0
     */
    @Test
    @DisplayName("Test Text Error Immediate")
    public void testTextErrorImmediate() throws Exception {
        Invocation.Builder request = client.target(generateURL("/text-error-immediate")).request();
        Response response = request.get();
        String entity = response.readEntity(String.class);
        Assertions.assertEquals(444, response.getStatus());
        Assertions.assertEquals(entity, "Got it");
        // make sure the completion callback was called with with an error
        request = client.target(generateURL("/callback-called-with-error/text-error-immediate")).request();
        response = request.get();
        Assertions.assertEquals(200, response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Resource method returns Publisher<String>, throws exception in stream.
     * @tpSince RESTEasy 4.0
     */
    @Test
    @DisplayName("Test Text Error Deferred")
    public void testTextErrorDeferred() throws Exception {
        Invocation.Builder request = client.target(generateURL("/text-error-deferred")).request();
        Response response = request.get();
        String entity = response.readEntity(String.class);
        Assertions.assertEquals(444, response.getStatus());
        Assertions.assertEquals(entity, "Got it");
        // make sure the completion callback was called with with an error
        request = client.target(generateURL("/callback-called-with-error/text-error-deferred")).request();
        response = request.get();
        Assertions.assertEquals(200, response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Resource method returns Publisher<String>.
     * @tpSince RESTEasy 4.0
     */
    @Test
    @DisplayName("Test Sse")
    public void testSse() throws Exception {
        WebTarget target = client.target(generateURL("/sse"));
        List<String> collector = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        CompletableFuture<Void> future = new CompletableFuture<Void>();
        SseEventSource source = SseEventSource.target(target).build();
        source.register(evt -> {
            String data = evt.readData(String.class);
            collector.add(data);
            if (collector.size() >= 2) {
                future.complete(null);
            }
        }, t -> {
            logger.error(t.getMessage(), t);
            errors.add(t);
        }, () -> {
            // bah, never called
            future.complete(null);
        });
        source.open();
        future.get();
        source.close();
        Assertions.assertEquals(2, collector.size());
        Assertions.assertEquals(0, errors.size());
        Assertions.assertEquals(collector.get(0), "one");
        Assertions.assertEquals(collector.get(1), "two");
    }

    /**
     * @tpTestDetails Resource method unsubscribes on close for infinite streams.
     * @tpSince RESTEasy 4.0
     */
    @Test
    @DisplayName("Test Infinite Streams Sse")
    public void testInfiniteStreamsSse() throws Exception {
        WebTarget target = client.target(generateURL("/sse-infinite"));
        List<String> collector = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        CompletableFuture<Void> future = new CompletableFuture<Void>();
        SseEventSource source = SseEventSource.target(target).build();
        source.register(evt -> {
            String data = evt.readData(String.class);
            collector.add(data);
            if (collector.size() >= 2) {
                future.complete(null);
            }
        }, t -> {
            logger.error(t);
            errors.add(t);
        }, () -> {
            // bah, never called
            future.complete(null);
        });
        source.open();
        future.get();
        source.close();
        Assertions.assertEquals(2, collector.size());
        Assertions.assertEquals(0, errors.size());
        Assertions.assertEquals(collector.get(0), "one");
        Assertions.assertEquals(collector.get(1), "one");
        close();
        setup();
        Thread.sleep(5000);
        Invocation.Builder request = client.target(generateURL("/infinite-done")).request();
        Response response = request.get();
        String entity = response.readEntity(String.class);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(entity, "true");
    }
}
