package io.quarkus.rest.test.providers.sse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;

import org.hamcrest.CoreMatchers;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.providers.sse.resource.SseSmokeResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

public class SseEventSourceTest {
    private static final Logger logger = Logger.getLogger(SseEventSourceTest.class);

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, SseSmokeResource.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, SseEventSourceTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test `SseEventSource.register(Consumer<InboundSseEvent> onEvent)`
     * @tpInfo RESTEASY-1680
     * @tpSince RESTEasy 3.5.0
     */
    @Test
    public void testSseEventSourceOnEventCallback() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final List<InboundSseEvent> results = new ArrayList<InboundSseEvent>();
        Client client = ClientBuilder.newBuilder().build();
        try {
            WebTarget target = client.target(generateURL("/sse/eventssimple"));
            SseEventSource msgEventSource = SseEventSource.target(target).build();
            try (SseEventSource eventSource = msgEventSource) {
                eventSource.register(event -> {
                    results.add(event);
                    latch.countDown();
                });
                eventSource.open();

                boolean waitResult = latch.await(30, TimeUnit.SECONDS);
                Assert.assertTrue("Waiting for event to be delivered has timed out.", waitResult);
            }
            Assert.assertEquals("One message was expected.", 1, results.size());
            Assert.assertThat("The message doesn't have expected content.", "data",
                    CoreMatchers.is(CoreMatchers.equalTo(results.get(0).readData(String.class))));
        } finally {
            client.close();
        }
    }

    /**
     * @tpTestDetails Test `SseEventSource.register(Consumer<InboundSseEvent> onEvent, Consumer<Throwable> onError)`
     * @tpInfo RESTEASY-1680
     * @tpSince RESTEasy 3.5.0
     */
    @Test
    public void testSseEventSourceOnEventOnErrorCallback() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final List<InboundSseEvent> results = new ArrayList<InboundSseEvent>();
        final AtomicInteger errors = new AtomicInteger(0);
        Client client = ClientBuilder.newBuilder().build();
        try {
            WebTarget target = client.target(generateURL("/sse/eventssimple"));
            SseEventSource msgEventSource = SseEventSource.target(target).build();
            try (SseEventSource eventSource = msgEventSource) {
                eventSource.register(event -> {
                    results.add(event);
                    latch.countDown();
                }, ex -> {
                    errors.incrementAndGet();
                    logger.error(ex.getMessage(), ex);
                    throw new RuntimeException(ex);
                });
                eventSource.open();

                boolean waitResult = latch.await(30, TimeUnit.SECONDS);
                Assert.assertTrue("Waiting for event to be delivered has timed out.", waitResult);
            }
            Assert.assertEquals("One message was expected.", 1, results.size());
            Assert.assertThat("The message doesn't have expected content.", "data",
                    CoreMatchers.is(CoreMatchers.equalTo(results.get(0).readData(String.class))));
        } finally {
            client.close();
        }
    }

    /**
     * @tpTestDetails Test `SseEventSource.register(Consumer<InboundSseEvent> onEvent, Consumer<Throwable> onError, Runnable
     *                onComplete)`
     * @tpInfo RESTEASY-1680
     * @tpSince RESTEasy 3.5.0
     */
    @Test
    public void testSseEventSourceOnEventOnErrorOnCompleteCallback() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final List<InboundSseEvent> results = new ArrayList<InboundSseEvent>();
        final AtomicInteger errors = new AtomicInteger(0);
        final AtomicInteger completed = new AtomicInteger(0);
        Client client = ClientBuilder.newBuilder().build();
        try {
            WebTarget target = client.target(generateURL("/sse/eventssimple"));
            SseEventSource msgEventSource = SseEventSource.target(target).build();
            try (SseEventSource eventSource = msgEventSource) {
                eventSource.register(event -> {
                    results.add(event);
                    latch.countDown();
                }, ex -> {
                    errors.incrementAndGet();
                    logger.error(ex.getMessage(), ex);
                    throw new RuntimeException(ex);
                }, () -> {
                    completed.incrementAndGet();
                });
                eventSource.open();

                boolean waitResult = latch.await(30, TimeUnit.SECONDS);
                Assert.assertTrue("Waiting for event to be delivered has timed out.", waitResult);
            }
            Assert.assertEquals(0, errors.get());
            Assert.assertEquals("One message was expected.", 1, results.size());
            Assert.assertThat("The message doesn't have expected content.", "data",
                    CoreMatchers.is(CoreMatchers.equalTo(results.get(0).readData(String.class))));
            Assert.assertEquals("On complete callback should be called one time", 1, completed.get());
        } finally {
            client.close();
        }
    }

}
