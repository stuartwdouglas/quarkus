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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.providers.sse.resource.SseReconnectResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

@DisplayName("Sse Reconnect Test")
public class SseReconnectTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

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
    @DisplayName("Test Reconnect Delay Is Not Set")
    public void testReconnectDelayIsNotSet() throws Exception {
        Client client = ClientBuilder.newClient();
        try {
            WebTarget baseTarget = client.target(generateURL("/reconnect/defaultReconnectDelay"));
            try (Response response = baseTarget.request().get()) {
                Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
                Assertions.assertEquals(SseEvent.RECONNECT_NOT_SET, (long) response.readEntity(long.class));
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
    @DisplayName("Test Reconnect Delay Is Set")
    public void testReconnectDelayIsSet() throws Exception {
        Client client = ClientBuilder.newClient();
        try {
            WebTarget baseTarget = client.target(generateURL("/reconnect/reconnectDelaySet"));
            try (Response response = baseTarget.request().get()) {
                Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
                Assertions.assertEquals(1000L, (long) response.readEntity(long.class));
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
    @DisplayName("Test Sse Endpoint Unavailable")
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
                    Assertions.assertTrue(ex instanceof ServiceUnavailableException, "ServiceUnavalile exception is expected");
                });
                eventSource.open();
                boolean waitResult = latch.await(30, TimeUnit.SECONDS);
                Assertions.assertEquals(1, errors.get());
                Assertions.assertTrue(waitResult, "Waiting for event to be delivered has timed out.");
            }
            Assertions.assertTrue(results.get(0).equals("ServiceAvailable"), "ServiceAvailable message is expected");
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
    @DisplayName("Test Reconnect Delay Is Used")
    public void testReconnectDelayIsUsed() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        List<InboundSseEvent> results = new ArrayList<>();
        AtomicInteger errorCount = new AtomicInteger();
        Client client = ClientBuilder.newBuilder().build();
        try {
            WebTarget target = client.target(generateURL("/reconnect/testReconnectDelayIsUsed"));
            SseEventSource sseEventSource = SseEventSource.target(target).reconnectingEvery(500, TimeUnit.MILLISECONDS).build();
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
                Assertions.assertTrue(waitResult, "Waiting for event to be delivered has timed out.");
                Assertions.assertEquals(0, errorCount.get());
                Assertions.assertEquals(2, results.size());
                Assertions.assertTrue(results.get(0).isReconnectDelaySet());
                Assertions.assertEquals(TimeUnit.SECONDS.toMillis(3), results.get(0).getReconnectDelay());
                Assertions.assertFalse(results.get(1).isReconnectDelaySet());
            }
        } finally {
            client.close();
        }
    }
}
