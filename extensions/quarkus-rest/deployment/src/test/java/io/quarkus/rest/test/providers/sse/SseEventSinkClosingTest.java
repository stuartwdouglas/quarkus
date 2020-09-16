package io.quarkus.rest.test.providers.sse;

import java.util.Arrays;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

@DisplayName("Sse Event Sink Closing Test")
public class SseEventSinkClosingTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(SseEventSinkClosingTestResource.class);
            war.addClass(SseEventSinkClosingTestResource.ContainerFilter.class);
            return TestUtil.finishContainerPrepare(war, null, Arrays.asList(SseEventSinkClosingTestResource.class),
                    SseEventSinkClosingTestResource.ContainerFilter.class);
        }
    });

    private String generateURL() {
        return PortProviderUtil.generateBaseUrl(SseEventSinkClosingTest.class.getSimpleName());
    }

    @AfterEach
    public void reset() throws Exception {
        Client client = ClientBuilder.newClient();
        try {
            client.target(generateURL()).path(SseEventSinkClosingTestResource.BASE_PATH)
                    .path(SseEventSinkClosingTestResource.RESET_RESPONSE_FILTER_INVOCATION_COUNT_PATH).request().delete();
        } finally {
            client.close();
        }
    }

    @Test
    @DisplayName("Test Filter For Event Sent")
    public void testFilterForEventSent() throws Exception {
        Client client = ClientBuilder.newClient();
        try {
            WebTarget baseTarget = client.target(generateURL()).path(SseEventSinkClosingTestResource.BASE_PATH);
            try (Response response = baseTarget.path(SseEventSinkClosingTestResource.SEND_AND_CLOSE_PATH)
                    .request(MediaType.SERVER_SENT_EVENTS_TYPE).get()) {
                Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            }
            try (Response response = baseTarget.path(SseEventSinkClosingTestResource.GET_RESPONSE_FILTER_INVOCATION_COUNT_PATH)
                    .request(MediaType.TEXT_PLAIN_TYPE).get()) {
                Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
                Assertions.assertEquals(Integer.valueOf(1), response.readEntity(Integer.class));
            }
        } finally {
            client.close();
        }
    }

    @Test
    @DisplayName("Test Filter For Method Return")
    public void testFilterForMethodReturn() throws Exception {
        Client client = ClientBuilder.newClient();
        try {
            WebTarget baseTarget = client.target(generateURL()).path(SseEventSinkClosingTestResource.BASE_PATH);
            try (Response response = baseTarget.path(SseEventSinkClosingTestResource.CLOSE_WITHOUT_SENDING_PATH)
                    .request(MediaType.SERVER_SENT_EVENTS_TYPE).get()) {
                Assertions.assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
            }
            try (Response response = baseTarget.path(SseEventSinkClosingTestResource.GET_RESPONSE_FILTER_INVOCATION_COUNT_PATH)
                    .request(MediaType.TEXT_PLAIN_TYPE).get()) {
                Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
                Assertions.assertEquals(Integer.valueOf(1), response.readEntity(Integer.class));
            }
        } finally {
            client.close();
        }
    }
}
