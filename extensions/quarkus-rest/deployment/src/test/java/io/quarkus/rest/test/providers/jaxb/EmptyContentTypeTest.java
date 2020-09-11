package io.quarkus.rest.test.providers.jaxb;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.jaxb.resource.EmptyContentTypeFoo;
import io.quarkus.rest.test.providers.jaxb.resource.EmptyContentTypeResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class EmptyContentTypeTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, EmptyContentTypeResource.class,
                            EmptyContentTypeFoo.class);
                }
            });

    @Before
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, EmptyContentTypeTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test for the resource with two post methods, one consumes xml content type the other consumes empty
     *                content type
     * @tpInfo RESTEASY-518
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testEmptyContentType() throws Exception {
        ResteasyWebTarget target = client.target(generateURL("/test"));
        EmptyContentTypeFoo foo = new EmptyContentTypeFoo();
        foo.setName("Bill");
        Response response = target.request().post(Entity.entity(foo, "application/xml"));
        Assert.assertEquals("The response from the server doesn't match the expected one",
                response.readEntity(String.class), "Bill");

        Response response2 = target.request().post(null);
        Assert.assertEquals("The response from the server doesn't match the expected one",
                response2.readEntity(String.class), "NULL");
    }

}
