package io.quarkus.rest.test.client.proxy;

import static org.junit.Assert.assertEquals;

import java.util.function.Supplier;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.client.proxy.resource.DefaultMediaTypesResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpSince RESTEasy 3.0.16
 */
public class DefaultMediaTypesTest {

    public interface Foo {
        @GET
        @Path("foo")
        String getFoo();

        @PUT
        @Path("foo")
        String setFoo(String value);
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(DefaultMediaTypesTest.class);
                    return TestUtil.finishContainerPrepare(war, null, DefaultMediaTypesResource.class);
                }
            });

    static QuarkusRestClient client;

    @Before
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, DefaultMediaTypesTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Client sends request thru client proxy, no default consumes type is specified.
     * @tpPassCrit Runtime exception is raised
     * @tpSince RESTEasy 3.0.16
     */
    @Test(expected = RuntimeException.class)
    public void testOldBehaviorContinues() throws Exception {
        ResteasyWebTarget target = client.target(generateURL("/foo"));
        target.proxy(Foo.class);
    }

    /**
     * @tpTestDetails Client sends request thru client proxy, the request has specified default produces and consumes type
     * @tpPassCrit The response contains acceptable media types set up by client (text/plain)
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testDefaultValues() throws Exception {
        ResteasyWebTarget target = client.target(generateURL("/foo"));
        Foo foo = target.proxyBuilder(Foo.class).defaultProduces(MediaType.TEXT_PLAIN_TYPE)
                .defaultConsumes(MediaType.TEXT_PLAIN_TYPE).build();

        assertEquals("The reponse header doesn't contain the expected media type", "[text/plain]", foo.getFoo());
        assertEquals("The reponse header doesn't contain the expected media type", "text/plain", foo.setFoo("SOMETHING"));
    }

    /**
     * @tpTestDetails Client sends request thru client proxy, the request has specified default produces and consumes type
     * @tpPassCrit The response contains acceptable media types set up by client (application/json)
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testMismatch() throws Exception {
        ResteasyWebTarget target = client.target(generateURL("/foo"));
        Foo foo = target.proxyBuilder(Foo.class).defaultProduces(MediaType.APPLICATION_JSON_TYPE)
                .defaultConsumes(MediaType.APPLICATION_JSON_TYPE).build();

        assertEquals("The reponse header doesn't contain the expected media type", "[application/json]", foo.getFoo());
        assertEquals("The reponse header doesn't contain the expected media type", "application/json", foo.setFoo("SOMETHING"));
    }
}
