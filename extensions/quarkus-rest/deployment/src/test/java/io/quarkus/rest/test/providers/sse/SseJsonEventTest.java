package io.quarkus.rest.test.providers.sse;

import static io.quarkus.rest.test.Assertions.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.rest.test.providers.sse.resource.SseSmokeResource;
import io.quarkus.rest.test.providers.sse.resource.SseSmokeUser;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;

@DisplayName("Sse Json Event Test")
public class SseJsonEventTest {

    private static final Logger logger = Logger.getLogger(SseJsonEventTest.class);

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = TestUtil.prepareArchive(SseJsonEventTest.class.getSimpleName());
        Map<String, String> contextParam = new HashMap<>();
        contextParam.put(ResteasyContextParameters.RESTEASY_PREFER_JACKSON_OVER_JSONB, "true");
        return TestUtil.finishContainerPrepare(war, contextParam, SseSmokeUser.class, SseSmokeResource.class);
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, SseJsonEventTest.class.getSimpleName());
    }

    @Test
    @DisplayName("Test Without Provider")
    public void testWithoutProvider() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final List<InboundSseEvent> results = new ArrayList<InboundSseEvent>();
        final AtomicInteger errors = new AtomicInteger(0);
        Client client = ClientBuilder.newClient();
        try {
            WebTarget target = client.target(generateURL("/sse/eventsjson"));
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
                Assertions.assertTrue(waitResult, "Waiting for event to be delivered has timed out.");
            }
        } finally {
            client.close();
        }
        Assertions.assertEquals(1, results.size(), "One message was expected.");
        try {
            results.get(0).readData(SseSmokeUser.class, MediaType.APPLICATION_JSON_TYPE);
            fail("Exception is expected");
        } catch (ProcessingException e) {
            Assertions.assertTrue(e.getMessage().indexOf("Failed to read data") > -1, "exception is not expected");
        }
    }

    @Test
    @DisplayName("Test Event With Custom Provider")
    public void testEventWithCustomProvider() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final List<InboundSseEvent> results = new ArrayList<InboundSseEvent>();
        final AtomicInteger errors = new AtomicInteger(0);
        Client client = ClientBuilder.newClient();
        try {
            WebTarget target = client.target(generateURL("/sse/eventsjson"));
            target.register(CustomJacksonProvider.class);
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
                Assertions.assertTrue(waitResult, "Waiting for event to be delivered has timed out.");
            }
            Assertions.assertEquals(1, results.size(), "One message was expected.");
            Assertions.assertEquals("Zeytin",
                    results.get(0).readData(SseSmokeUser.class, MediaType.APPLICATION_JSON_TYPE).getUsername(),
                    "user name is not expected");
        } finally {
            client.close();
        }
    }
}
