package io.quarkus.rest.test.core.basic;

import java.util.function.Supplier;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;

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
import io.quarkus.rest.test.core.basic.resource.CacheControlAnnotationResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Configuration
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for org.jboss.resteasy.annotations.cache.Cache class
 * @tpSince RESTEasy 3.0.16
 */
public class CacheControlAnnotationTest {

    private static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
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

    @Before
    public void setup() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Test for correct value of max-age of cache annotation
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testResourceValid() {
        WebTarget base = client.target(generateURL("/maxage"));
        Response response = base.request().get();

        Assert.assertEquals(Status.OK, response.getStatus());
        CacheControl cc = CacheControl.valueOf(response.getHeaderString("cache-control"));
        Assert.assertFalse("Cache should not be private", cc.isPrivate());
        Assert.assertEquals("Wrong age of cache", 3600, cc.getMaxAge());

        response.close();
    }

    /**
     * @tpTestDetails Test for no-cache settings
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testResourceNoCach() {
        WebTarget base = client.target(generateURL("/nocache"));
        Response response = base.request().get();

        Assert.assertEquals(Status.OK, response.getStatus());
        String value = response.getHeaderString("cache-control");
        Assert.assertEquals("Wrong value of cache header", "no-cache", value);
        CacheControl cc = CacheControl.valueOf(value);
        Assert.assertTrue("Wrong value of cache header", cc.isNoCache());

        response.close();
    }

    /**
     * @tpTestDetails Test for no-cache settings mixed with other directives
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testResourceCompositeNoCache() {
        WebTarget base = client.target(generateURL("/composite"));
        Response response = base.request().get();

        Assert.assertEquals(Status.OK, response.getStatus());
        CacheControl cc = CacheControl.valueOf(response.getHeaderString("cache-control"));
        Assert.assertTrue("There must be no-store", cc.isNoStore());
        Assert.assertTrue("There must be must-revalidate", cc.isMustRevalidate());
        Assert.assertTrue("Cache must be private", cc.isPrivate());
        Assert.assertEquals("Wrong age of cache", 0, cc.getMaxAge());
        Assert.assertEquals("Wrong age of shared cache", 0, cc.getSMaxAge());
        Assert.assertTrue("There must be no-cache", cc.isNoCache());
        response.close();
    }

}
