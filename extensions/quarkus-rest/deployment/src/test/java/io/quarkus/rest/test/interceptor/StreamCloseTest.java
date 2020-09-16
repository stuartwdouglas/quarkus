package io.quarkus.rest.test.interceptor;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.rest.test.interceptor.resource.InterceptorStreamResource;
import io.quarkus.rest.test.interceptor.resource.TestInterceptor;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;

/**
 * @tpSubChapter Interceptors
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.1.2
 * @tpTestCaseDetails Verify outpustream close is invoked on server side (https://issues.jboss.org/browse/RESTEASY-1650)
 */
@DisplayName("Stream Close Test")
public class StreamCloseTest {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = TestUtil.prepareArchive(StreamCloseTest.class.getSimpleName());
        return TestUtil.finishContainerPrepare(war, null, InterceptorStreamResource.class, TestInterceptor.class,
                PortProviderUtil.class);
    }

    static Client client;

    @BeforeEach
    public void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterEach
    public void cleanup() {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, StreamCloseTest.class.getSimpleName());
    }

    @Test
    @DisplayName("Test Priority")
    public void testPriority() throws Exception {
        final int count = TestInterceptor.closeCounter.get();
        Response response = client.target(generateURL("/test")).request().post(Entity.text("test"));
        response.bufferEntity();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus(),
                "Wrong response status, interceptors don't work correctly");
        Assertions.assertEquals("test", response.readEntity(String.class),
                "Wrong content of response, interceptors don't work correctly");
        response.close();
        Assertions.assertEquals(1, TestInterceptor.closeCounter.get() - count);
    }
}
