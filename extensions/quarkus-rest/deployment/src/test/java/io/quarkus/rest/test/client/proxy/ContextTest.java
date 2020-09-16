package io.quarkus.rest.test.client.proxy;

import java.util.function.Supplier;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.client.proxy.resource.ContextTestResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Context Test")
public class ContextTest {

    @Path(value = "/test")
    @DisplayName("Resource Interface")
    public interface ResourceInterface {

        @GET
        @Produces("text/plain")
        String echo(@Context UriInfo info);
    }

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(ContextTest.class);
            return TestUtil.finishContainerPrepare(war, null, ContextTestResource.class);
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
        return PortProviderUtil.generateURL(path, ContextTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Client sends async GET requests thru client proxy. UriInfo is injected as argument of the GET
     *                method call.
     * @tpPassCrit UriInfo was injected into method call
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Context Injection Proxy")
    public void testContextInjectionProxy() {
        ResourceInterface proxy = client.target(generateURL("")).proxy(ResourceInterface.class);
        Assertions.assertEquals("content", proxy.echo(null), "UriInfo was not injected");
    }
}
