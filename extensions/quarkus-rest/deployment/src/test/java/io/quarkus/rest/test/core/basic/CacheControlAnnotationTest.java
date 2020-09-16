package io.quarkus.rest.test.core.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.core.basic.resource.CacheControlAnnotationResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Configuration
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for io.quarkus.rest.Cache class
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Cache Control Annotation Test")
public class CacheControlAnnotationTest {

    private static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, CacheControlAnnotationResource.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, CacheControlAnnotationTest.class.getSimpleName());
    }

    @BeforeEach
    public void setup() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Test for correct value of max-age of cache annotation
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Resource Valid")
    public void testResourceValid() {
        WebTarget base = client.target(generateURL("/maxage"));
        Response response = base.request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        CacheControl cc = CacheControl.valueOf(response.getHeaderString("cache-control"));
        Assertions.assertFalse(cc.isPrivate(), "Cache should not be private");
        Assertions.assertEquals(3600, cc.getMaxAge(), "Wrong age of cache");
        response.close();
    }

    /**
     * @tpTestDetails Test for no-cache settings
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Resource No Cach")
    public void testResourceNoCach() {
        WebTarget base = client.target(generateURL("/nocache"));
        Response response = base.request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String value = response.getHeaderString("cache-control");
        Assertions.assertEquals("no-cache", value, "Wrong value of cache header");
        CacheControl cc = CacheControl.valueOf(value);
        Assertions.assertTrue(cc.isNoCache(), "Wrong value of cache header");
        response.close();
    }

    /**
     * @tpTestDetails Test for no-cache settings mixed with other directives
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    @DisplayName("Test Resource Composite No Cache")
    public void testResourceCompositeNoCache() {
        WebTarget base = client.target(generateURL("/composite"));
        Response response = base.request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        CacheControl cc = CacheControl.valueOf(response.getHeaderString("cache-control"));
        Assertions.assertTrue(cc.isNoStore(), "There must be no-store");
        Assertions.assertTrue(cc.isMustRevalidate(), "There must be must-revalidate");
        Assertions.assertTrue(cc.isPrivate(), "Cache must be private");
        Assertions.assertEquals(0, cc.getMaxAge(), "Wrong age of cache");
        Assertions.assertEquals(0, cc.getSMaxAge(), "Wrong age of shared cache");
        Assertions.assertTrue(cc.isNoCache(), "There must be no-cache");
        response.close();
    }
}
