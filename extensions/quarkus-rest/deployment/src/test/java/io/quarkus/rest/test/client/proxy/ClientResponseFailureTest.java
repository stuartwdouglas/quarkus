package io.quarkus.rest.test.client.proxy;

import java.util.function.Supplier;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.Assert;
import io.quarkus.rest.test.client.proxy.resource.ClientResponseFailureResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class ClientResponseFailureTest {

    @Path("/test")
    public interface ClientResponseFailureResourceInterface {
        @GET
        @Produces("text/plain")
        String get();

        @GET
        @Path("error")
        @Produces("text/plain")
        String error();
    }

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(ClientResponseFailureTest.class);
                    return TestUtil.finishContainerPrepare(war, null, ClientResponseFailureResource.class);
                }
            });

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ClientResponseFailureTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Client sends async GET requests thru client proxy. The NotFoundException should be thrown as response.
     * @tpPassCrit Exception NotFoundException is thrown
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testStreamStillOpen() throws Exception {

        final ClientResponseFailureResourceInterface proxy = client.target(generateURL(""))
                .proxy(ClientResponseFailureResourceInterface.class);
        boolean failed = true;
        try {
            proxy.error();
            failed = false;
        } catch (NotFoundException e) {
            Assert.assertEquals(Status.NOT_FOUND.getStatusCode(), e.getResponse().getStatus());
            Assert.assertEquals("There wasn't expected message", e.getResponse().readEntity(String.class),
                    "there was an error");
            e.getResponse().close();
        }

        Assert.assertTrue("The expected NotFoundException didn't happened", failed);
    }
}
