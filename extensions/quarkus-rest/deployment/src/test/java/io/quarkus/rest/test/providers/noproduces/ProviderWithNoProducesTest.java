package io.quarkus.rest.test.providers.noproduces;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.providers.noproduces.resource.Foo;
import io.quarkus.rest.test.providers.noproduces.resource.ProviderWithNoProducesMessageBodyWriter;
import io.quarkus.rest.test.providers.noproduces.resource.ProviderWithNoProducesResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter MessageBodyWriters with no @Produces annotation
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-2232
 * @tpSince RESTEasy 4.0.0
 */
public class ProviderWithNoProducesTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(Foo.class);
                    war.addAsWebInfResource(ProviderWithNoProducesTest.class.getPackage(), "ProviderWithNoProduces_web.xml",
                            "web.xml");
                    return TestUtil.finishContainerPrepare(war, null, ProviderWithNoProducesResource.class,
                            ProviderWithNoProducesMessageBodyWriter.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ProviderWithNoProducesTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails ProviderWithNoProducesMessageBodyWriter, which has no @Pruduces annotation, should
     *                be williing to write a Foo with media type "foo/bar" but not "bar/foo".
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testWriteFoo() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(generateURL("/foo"));
        Response response = target.request().accept("foo/bar;q=0.9, bar/foo;q=1.0").get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("Wrong response content", "ProviderWithNoProducesMessageBodyWriter",
                response.readEntity(String.class));
        client.close();
    }
}
