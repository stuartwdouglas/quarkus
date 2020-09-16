package io.quarkus.rest.test.client;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.ClientBuilder;

import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestClientBuilder;
import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.client.resource.TimeoutResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpSince RESTEasy 3.0.16
 */
public class TimeoutTest extends ClientTestBase {
    @Path("/timeout")
    public interface TimeoutResourceInterface {
        @GET
        @Produces("text/plain")
        String get(@QueryParam("sleep") int sleep) throws Exception;
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(TimeoutTest.class);
                    war.addClass(ClientTestBase.class);
                    return TestUtil.finishContainerPrepare(war, null, TimeoutResource.class);
                }
            });

    /**
     * @tpTestDetails Create client with custom SocketTimeout setting. Client sends GET request for the resource which
     *                calls sleep() for the specified amount of time.
     * @tpPassCrit The request gets timeouted
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testTimeout() throws Exception {
        QuarkusRestClient clientengine = ((QuarkusRestClientBuilder) ClientBuilder.newBuilder())
                .readTimeout(2, TimeUnit.SECONDS).build();
        ClientHttpEngine engine = clientengine.httpEngine();
        Assert.assertNotNull("Client engine is was not created", engine);

        QuarkusRestClient client = ((QuarkusRestClientBuilder) ClientBuilder.newBuilder()).httpEngine(engine).build();
        QuarkusRestWebTarget target = client.target(generateURL("/timeout"));
        try {
            target.queryParam("sleep", "5").request().get();
            Assert.fail("The request didn't timeout as expected");
        } catch (ProcessingException e) {
            Assert.assertEquals("Expected SocketTimeoutException", e.getCause().getClass(), SocketTimeoutException.class);
        }

        TimeoutResourceInterface proxy = client.target(generateURL("")).proxy(TimeoutResourceInterface.class);
        try {
            proxy.get(5);
            Assert.fail("The request didn't timeout as expected when using client proxy");
        } catch (ProcessingException e) {
            Assert.assertEquals("Expected SocketTimeoutException", e.getCause().getClass(), SocketTimeoutException.class);
        }
        clientengine.close();
    }
}
