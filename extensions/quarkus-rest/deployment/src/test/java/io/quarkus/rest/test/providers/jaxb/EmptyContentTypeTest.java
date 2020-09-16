package io.quarkus.rest.test.providers.jaxb;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
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
@DisplayName("Empty Content Type Test")
public class EmptyContentTypeTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, EmptyContentTypeResource.class, EmptyContentTypeFoo.class);
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
        return PortProviderUtil.generateURL(path, EmptyContentTypeTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test for the resource with two post methods, one consumes xml content type the other consumes empty
     *                content type
     * @tpInfo RESTEASY-518
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Empty Content Type")
    public void testEmptyContentType() throws Exception {
        QuarkusRestWebTarget target = client.target(generateURL("/test"));
        EmptyContentTypeFoo foo = new EmptyContentTypeFoo();
        foo.setName("Bill");
        Response response = target.request().post(Entity.entity(foo, "application/xml"));
        Assertions.assertEquals(response.readEntity(String.class), "Bill",
                "The response from the server doesn't match the expected one");
        Response response2 = target.request().post(null);
        Assertions.assertEquals(response2.readEntity(String.class), "NULL",
                "The response from the server doesn't match the expected one");
    }
}
