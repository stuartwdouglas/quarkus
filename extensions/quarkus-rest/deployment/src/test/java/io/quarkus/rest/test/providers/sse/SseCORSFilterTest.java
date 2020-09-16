package io.quarkus.rest.test.providers.sse;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;

@DisplayName("Sse CORS Filter Test")
public class SseCORSFilterTest {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = TestUtil.prepareArchive(SseCORSFilterTest.class.getSimpleName());
        war.addClass(SseCORSFilterTest.class);
        // war.addAsWebInfResource("org/jboss/resteasy/test/providers/sse/filter/web.xml", "web.xml");
        war.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return TestUtil.finishContainerPrepare(war, null, SseFilterApplication.class, SseResource.class, CORSFilter.class,
                ExecutorServletContextListener.class);
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, SseCORSFilterTest.class.getSimpleName());
    }

    @Test
    @DisplayName("Test Sse Event With Filter")
    public void testSseEventWithFilter() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(generateURL("/service/server-sent-events/events"));
        Response response = target.request().get();
        Assertions.assertEquals(200, response.getStatus(), "response OK is expected");
        Assertions.assertTrue(response.getHeaders().get("Access-Control-Allow-Origin").contains("*"),
                "CORS http header is expected in event response");
        MediaType mt = response.getMediaType();
        mt = new MediaType(mt.getType(), mt.getSubtype());
        Assertions.assertEquals(mt, MediaType.SERVER_SENT_EVENTS_TYPE, "text/event-stream is expected");
        Client isOpenClient = ClientBuilder.newClient();
        Invocation.Builder isOpenRequest = isOpenClient.target(generateURL("/service/server-sent-events/isopen")).request();
        javax.ws.rs.core.Response isOpenResponse = isOpenRequest.get();
        Assertions.assertTrue(isOpenResponse.readEntity(Boolean.class), "EventSink open is expected ");
        Assertions.assertTrue(isOpenResponse.getHeaders().get("Access-Control-Allow-Origin").contains("*"),
                "CORS http header is expected in isOpenResponse");
        isOpenClient.close();
        client.close();
    }
}
