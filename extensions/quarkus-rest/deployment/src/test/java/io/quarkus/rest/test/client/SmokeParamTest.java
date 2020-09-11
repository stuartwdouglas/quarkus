package io.quarkus.rest.test.client;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.client.resource.SmokeParamResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpTestCaseDetails Smoke parameter test.
 * @tpSince RESTEasy 3.0.16
 */
public class SmokeParamTest extends ClientTestBase {

    static Client client;

    @BeforeClass
    public static void setup() throws Exception {
        client = ClientBuilder.newClient();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, SmokeParamResource.class);
                }
            });

    @AfterClass
    public static void close() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Test one request with header and query parameter.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testSimple() throws Exception {
        Response response = client.target(generateURL("/foo")).request()
                .post(Entity.entity("hello world", "text/plain"));
        Assert.assertEquals("hello world", response.readEntity(String.class));
        response.close();

        response = client.target(generateURL("/foo")).queryParam("b", "world").request()
                .header("a", "hello")
                .get();
        Assert.assertEquals("hello world", response.readEntity(String.class));
        response.close();

    }
}
