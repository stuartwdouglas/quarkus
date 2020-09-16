package io.quarkus.rest.test.response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.SseEventSource;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
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
@DisplayName("Another Publisher Response No Stream Test")
public class AnotherPublisherResponseNoStreamTest {

    private static final Logger logger = Logger.getLogger(AnotherPublisherResponseNoStreamTest.class);

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.setManifest(new StringAsset("Manifest-Version: 1.0\n"
                    + "Dependencies: org.jboss.resteasy.resteasy-rxjava2 services, org.reactivestreams\n"));
            return TestUtil.finishContainerPrepare(war, null, PublisherResponseNoStreamResource.class,
                    AsyncResponseCallback.class, AsyncResponseExceptionMapper.class, AsyncResponseException.class,
                    PortProviderUtil.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, AnotherPublisherResponseNoStreamTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Resource method returns Publisher<String>.
     * @tpSince RESTEasy 4.0
     */
    @Test
    @DisplayName("Test Sse")
    public void testSse() throws Exception {
        for (int i = 0; i < 40; i++) {
            internalTestSse(i);
        }
    }

    public void internalTestSse(int i) throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(generateURL("/sse"));
        List<String> collector = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        CompletableFuture<Void> future = new CompletableFuture<Void>();
        try (SseEventSource source = SseEventSource.target(target).build()) {
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
            future.get(5000, TimeUnit.SECONDS);
            Assertions.assertEquals(2, collector.size());
            Assertions.assertEquals(0, errors.size());
            Assertions.assertTrue(collector.contains("one"));
            Assertions.assertTrue(collector.contains("two"));
        }
        client.close();
    }
}
