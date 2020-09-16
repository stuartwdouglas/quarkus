package io.quarkus.rest.test.providers.sse;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;

/**
 * @author Nicolas NESMON
 */
@DisplayName("Sse Post Resource Method Invoker Test")
public class SsePostResourceMethodInvokerTest {

    private static final String WITHOUT_EXCEPTION_REQUEST_FILTER = "withoutExceptionRequestFilter";

    private static final String WITH_EXCEPTION_REQUEST_FILTER = "withExceptionRequestFilter";

    @Deployment(name = WITH_EXCEPTION_REQUEST_FILTER)
    public static Archive<?> deployWithoutExceptionRequestFilter() throws Exception {
        WebArchive war = TestUtil.prepareArchive(WITH_EXCEPTION_REQUEST_FILTER);
        return TestUtil.finishContainerPrepare(war, null, Arrays.asList(SsePostResourceMethodInvokerTestResource.class),
                SsePostResourceMethodInvokerTestResource.ExceptionRequestFilter.class);
    }

    @Deployment(name = WITHOUT_EXCEPTION_REQUEST_FILTER)
    public static Archive<?> deployWithExceptionRequestFilter() throws Exception {
        WebArchive war = TestUtil.prepareArchive(WITHOUT_EXCEPTION_REQUEST_FILTER);
        // war.addAsWebInfResource("org/jboss/resteasy/test/providers/sse/synch-web.xml", "web.xml");
        war.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return TestUtil.finishContainerPrepare(war, null, SsePostResourceMethodInvokerTestResource.class,
                SsePostResourceMethodInvokerApplication.class);
    }

    private static String generateURL(String tesName) {
        return PortProviderUtil.generateBaseUrl(tesName);
    }

    @Test
    @OperateOnDeployment(WITHOUT_EXCEPTION_REQUEST_FILTER)
    @DisplayName("Should _ Return 200 Ok Response _ When _ Resource Method Exit First")
    public void Should_Return200OkResponse_When_ResourceMethodExitFirst() throws Exception {
        Client client = ClientBuilder.newClient();
        try {
            WebTarget baseTarget = client.target(generateURL(WITHOUT_EXCEPTION_REQUEST_FILTER))
                    .path(SsePostResourceMethodInvokerTestResource.BASE_PATH);
            try {
                Response response = baseTarget.request(MediaType.SERVER_SENT_EVENTS_TYPE).buildGet().submit().get(10,
                        TimeUnit.SECONDS);
                try {
                    Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
                } finally {
                    client.close();
                }
            } catch (TimeoutException e) {
                Assertions.fail("Sse initial 200 ok response is expected when resource method resource returns first.");
            }
        } finally {
            client = ClientBuilder.newClient();
            try {
                client.target(generateURL(WITHOUT_EXCEPTION_REQUEST_FILTER))
                        .path(SsePostResourceMethodInvokerTestResource.BASE_PATH)
                        .path(SsePostResourceMethodInvokerTestResource.CLOSE_PATH).request().delete();
            } finally {
                client.close();
            }
        }
    }

    @Test
    @OperateOnDeployment(WITH_EXCEPTION_REQUEST_FILTER)
    @DisplayName("Should _ Throw Intenal Server Error _ When _ Any Filter After Sse Filter Throws IO Exception")
    public void Should_ThrowIntenalServerError_When_AnyFilterAfterSseFilterThrowsIOException() throws Exception {
        assertThrows(InternalServerErrorException.class, () -> {
            Client client = ClientBuilder.newClient();
            try {
                client.target(generateURL(WITH_EXCEPTION_REQUEST_FILTER))
                        .path(SsePostResourceMethodInvokerTestResource.BASE_PATH).request(MediaType.SERVER_SENT_EVENTS_TYPE)
                        .get(String.class);
            } finally {
                client.close();
            }
        });
    }
}
