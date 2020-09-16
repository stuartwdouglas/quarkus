package io.quarkus.rest.test.cache;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.ClientBuilder;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.resteasy.category.ExpectedFailingOnWildFly19;
import org.jboss.resteasy.plugins.cache.server.InfinispanCache;
import org.jboss.resteasy.plugins.cache.server.ServerCache;
import org.jboss.resteasy.plugins.cache.server.ServerCacheFeature;
import org.jboss.resteasy.plugins.cache.server.ServerCacheHitFilter;
import org.jboss.resteasy.plugins.cache.server.ServerCacheInterceptor;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.cache.resource.ServerCacheInterceptorResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;

/**
 * @tpSubChapter RESTEasy Cache Core
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1423
 * @tpSince RESTEasy 3.0.16
 */
@Category({ ExpectedFailingOnWildFly19.class })
public class ServerCacheInterceptorTest {

    private static QuarkusRestClient clientA;
    private static QuarkusRestClient clientB;

    @Deployment
    public static Archive<?> deploySimpleResource() {
        List<Class<?>> singletons = new ArrayList<>();
        singletons.add(ServerCacheFeature.class);
        WebArchive war = TestUtil.prepareArchive(ServerCacheInterceptorTest.class.getSimpleName());
        // This test is not supposed to run with security manager

        war.addClasses(ServerCache.class, InfinispanCache.class, ServerCacheHitFilter.class, ServerCacheInterceptor.class);
        war.addAsManifestResource(new StringAsset("Manifest-Version: 1.0\n" + "Dependencies: org.infinispan\n"), "MANIFEST.MF");
        return TestUtil.finishContainerPrepare(war, null, singletons, ServerCacheInterceptorResource.class);
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ServerCacheInterceptorTest.class.getSimpleName());
    }

    @Before
    public void setup() {
        clientA = (QuarkusRestClient) ClientBuilder.newClient();
        clientB = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @After
    public void after() throws Exception {
        clientA.close();
        clientB.close();
    }

    /**
     * @tpTestDetails Verifies that a 'public' resource is cached by the server side cache.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void cachePublicResource() {
        String responseA = clientA.target(generateURL("/public")).request().get(String.class);
        String responseB = clientB.target(generateURL("/public")).request().get(String.class);
        Assert.assertEquals(responseA, responseB);
    }

    /**
     * @tpTestDetails Verifies that a 'private' resource is not cached by the server side cache.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void doNotCachePrivateResource() {
        String responseA = clientA.target(generateURL("/private")).request().get(String.class);
        String responseB = clientB.target(generateURL("/private")).request().get(String.class);
        Assert.assertNotEquals(responseA, responseB);
    }

    /**
     * @tpTestDetails Verifies that a resource marked with the 'no-store' directive is not cached by the server side cache.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void doNotCacheNoStoreResource() {
        String responseA = clientA.target(generateURL("/no-store")).request().get(String.class);
        String responseB = clientB.target(generateURL("/no-store")).request().get(String.class);
        Assert.assertNotEquals(responseA, responseB);
    }

}
