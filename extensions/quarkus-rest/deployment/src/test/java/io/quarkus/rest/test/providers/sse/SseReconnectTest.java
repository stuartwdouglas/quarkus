package io.quarkus.rest.test.providers.sse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEvent;
import javax.ws.rs.sse.SseEventSource;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.providers.sse.resource.SseReconnectResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

public class SseReconnectTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, SseReconnectResource.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, SseReconnectTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Check SseEvent.getReconnectDelay() returns SseEvent.RECONNECT_NOT_SET,
     *                when OutboundSseEvent.reconnectDelay() is not set.
     * @tpInfo RESTEASY-1680
     * @tpSince RESTEasy 3.5.0
     */
    @Test
    public void testReconnectDelayIsNotSet() throws Exception {
        Client client = ClientBuilder.newClient();
        try {
            WebTarget baseTarget = client.target(generateURL("/reconnect/defaultReconnectDelay"));
            try (Response response = baseTarget.request().get()) {
                Assert.assertEquals(Status.OK, response.getStatus());
                Assert.assertEquals(SseEvent.RECONNECT_NOT_SET, (long) response.readEntity(long.class));
            }
        } finally {
            client.close();
        }
    }

    /**
     * @tpTestDetails *** Check SseEvent.getReconnectDelay() returns correct delay,
     *                when OutboundSseEvent.reconnectDelay() is set.
     * @tpInfo RESTEASY-1680
     * @tpSince RESTEasy 3.5.0
     */
    @Test
    public void testReconnectDelayIsSet() throws Exception {
        Client client = ClientBuilder.newClient();
        try {
            WebTarget baseTarget = client.target(generateURL("/reconnect/reconnectDelaySet"));
            try (Response response = baseTarget.request().get()) {
                Assert.assertEquals(Status.OK, response.getStatus());
                Assert.assertEquals(1000L, (long) response.readEntity(long.class));
            }
        } finally {
            client.close();
        }
    }

    /**
     * @tpTestDetails SseEventSource receives HTTP 503 + "Retry-After" from the SSE endpoint. Check that SseEventSource
     *                retries after the specified period
     * @tpInfo RESTEASY-1680
     * @tpSince RESTEasy 3.5.0
     */
    @Test
    public void testSseEndpointUnavailable() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger errors = new AtomicInteger(0);
        final List<String> results = new ArrayList<String>();
        Client client = ClientBuilder.newBuilder().build();
        try {
            WebTarget target = client.target(generateURL("/reconnect/unavailable"));
            SseEventSource msgEventSource = SseEventSource.target(target).build();
            try (SseEventSource eventSource = msgEventSource) {
                eventSource.register(event -> {
                    results.add(event.readData(String.class));
                    latch.countDown();
                }, ex -> {
                    errors.incrementAndGet();
                    Assert.assertTrue("ServiceUnavalile exception is expected", ex instanceof ServiceUnavailableException);
                });
                eventSource.open();

                boolean waitResult = latch.await(30, TimeUnit.SECONDS);
                Assert.assertEquals(1, errors.get());
                Assert.assertTrue("Waiting for event to be delivered has timed out.", waitResult);
            }
            Assert.assertTrue("ServiceAvailable message is expected", results.get(0).equals("ServiceAvailable"));
        } finally {
            client.close();
        }
    }

    /**
     * @tpTestDetails Check that SseEventSource use last received 'retry' field value as the default reconnect delay for all
     *                future requests.
     * @tpInfo RESTEASY-1958
     * @tpSince RESTEasy
     */
    @Test
    public void testReconnectDelayIsUsed() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        List<InboundSseEvent> results = new ArrayList<>();
        AtomicInteger errorCount = new AtomicInteger();
        Client client = ClientBuilder.newBuilder().build();
        try {
            WebTarget target = client.target(generateURL("/reconnect/testReconnectDelayIsUsed"));
            SseEventSource sseEventSource = SseEventSource.target(target).reconnectingEvery(500, TimeUnit.MILLISECONDS)
                    .build();
            sseEventSource.register(event -> {
                results.add(event);
            }, error -> {
                if (error instanceof WebApplicationException) {
                    if (599 == ((WebApplicationException) error).getResponse().getStatus()) {
                        return;
                    }
                }
                errorCount.incrementAndGet();
            }, () -> {
                latch.countDown();
            });
            try (SseEventSource eventSource = sseEventSource) {
                eventSource.open();
                boolean waitResult = latch.await(30, TimeUnit.SECONDS);
                Assert.assertTrue("Waiting for event to be delivered has timed out.", waitResult);
                Assert.assertEquals(0, errorCount.get());
                Assert.assertEquals(2, results.size());
                Assert.assertTrue(results.get(0).isReconnectDelaySet());
                Assert.assertEquals(TimeUnit.SECONDS.toMillis(3), results.get(0).getReconnectDelay());
                Assert.assertFalse(results.get(1).isReconnectDelaySet());
            }
        } finally {
            client.close();
        }
    }

}
