package io.quarkus.rest.test.interceptor;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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

public class StreamCloseTest {
    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = TestUtil.prepareArchive(StreamCloseTest.class.getSimpleName());

        return TestUtil.finishContainerPrepare(war, null, InterceptorStreamResource.class, TestInterceptor.class,
                PortProviderUtil.class);
    }

    static Client client;

    @Before
    public void setup() {
        client = ClientBuilder.newClient();
    }

    @After
    public void cleanup() {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, StreamCloseTest.class.getSimpleName());
    }

    @Test
    public void testPriority() throws Exception {
        final int count = TestInterceptor.closeCounter.get();
        Response response = client.target(generateURL("/test")).request().post(Entity.text("test"));
        response.bufferEntity();
        Assert.assertEquals("Wrong response status, interceptors don't work correctly", Status.OK.getStatusCode(),
                response.getStatus());
        Assert.assertEquals("Wrong content of response, interceptors don't work correctly", "test",
                response.readEntity(String.class));
        response.close();
        Assert.assertEquals(1, TestInterceptor.closeCounter.get() - count);

    }
}
