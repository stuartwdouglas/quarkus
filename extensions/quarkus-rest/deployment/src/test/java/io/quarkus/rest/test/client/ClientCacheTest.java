package io.quarkus.rest.test.client;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.jboss.resteasy.client.jaxrs.cache.BrowserCache;
import org.jboss.resteasy.client.jaxrs.cache.BrowserCacheFeature;
import org.jboss.resteasy.client.jaxrs.cache.LightweightBrowserCache;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.client.resource.ClientCacheProxy;
import io.quarkus.rest.test.client.resource.ClientCacheService;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpSince RESTEasy 3.0.16
 * @tpTestCaseDetails Test for client cache
 */

public class ClientCacheTest {

    public static AtomicInteger count = new AtomicInteger(0);

    @Before
    public void setUp() throws Exception {
        count.set(0);
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(ClientCacheProxy.class, ClientCacheTest.class, TestUtil.class, PortProviderUtil.class);
                    // Arquillian in the deployment and use of PortProviderUtil and Test util in the deployment

                    war.addClasses(ClientCacheProxy.class, ClientCacheTest.class, TestUtil.class, PortProviderUtil.class);
                    return TestUtil.finishContainerPrepare(war, null, ClientCacheService.class);
                }
            });

    private String generateBaseUrl() {
        return PortProviderUtil.generateBaseUrl(ClientCacheTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Complex proxy test for client cache
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testProxy() throws Exception {
        count.set(0);
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        QuarkusRestWebTarget target = client.target(generateBaseUrl());
        target.register(BrowserCacheFeature.class);

        ClientCacheProxy proxy = target.proxy(ClientCacheProxy.class);
        String rtn;
        rtn = proxy.get();
        Assert.assertEquals("Wrong response", "hello world" + 1, rtn);
        Assert.assertEquals("Wrong cache size", 1, count.get());
        rtn = proxy.get();
        Assert.assertEquals("Wrong response", "hello world" + 1, rtn);
        Assert.assertEquals("Wrong cache size", 1, count.get());
        Thread.sleep(2000);
        rtn = proxy.get();
        Assert.assertEquals("Wrong response", "hello world" + 2, rtn);
        Assert.assertEquals("Wrong cache size", 2, count.get());
        rtn = proxy.get();
        Assert.assertEquals("Wrong response", "hello world" + 2, rtn);
        Assert.assertEquals("Wrong cache size", 2, count.get());

        // Test always good etag
        count.set(0);
        rtn = proxy.getAlwaysGoodEtag();
        Assert.assertEquals("Wrong response", "hello1", rtn);
        Assert.assertEquals("Wrong cache size", 1, count.get());
        rtn = proxy.getAlwaysGoodEtag();
        Assert.assertEquals("Wrong response", "hello1", rtn);
        Assert.assertEquals("Wrong cache size", 1, count.get());
        Thread.sleep(2000);
        rtn = proxy.getAlwaysGoodEtag();
        Assert.assertEquals("Wrong response", "hello1", rtn);
        Assert.assertEquals("Wrong cache size", 2, count.get());
        rtn = proxy.getAlwaysGoodEtag();
        Assert.assertEquals("Wrong response", "hello1", rtn);
        Assert.assertEquals("Wrong cache size", 2, count.get());

        // Test never good etag
        count.set(0);
        rtn = proxy.getNeverGoodEtag();
        Assert.assertEquals("Wrong response", "hello1", rtn);
        Assert.assertEquals("Wrong cache size", 1, count.get());
        rtn = proxy.getNeverGoodEtag();
        Assert.assertEquals("Wrong response", "hello1", rtn);
        Assert.assertEquals("Wrong cache size", 1, count.get());
        Thread.sleep(2000);
        rtn = proxy.getNeverGoodEtag();
        Assert.assertEquals("Wrong response", "hello2", rtn);
        Assert.assertEquals("Wrong cache size", 2, count.get());
        rtn = proxy.getNeverGoodEtag();
        Assert.assertEquals("Wrong response", "hello2", rtn);
        Assert.assertEquals("Wrong cache size", 2, count.get());

        // Test always validate etag
        count.set(0);
        rtn = proxy.getValidateEtagged();
        Assert.assertEquals("Wrong response", "hello1", rtn);
        Assert.assertEquals("Wrong cache size", 1, count.get());
        rtn = proxy.getValidateEtagged();
        Assert.assertEquals("Wrong response", "hello1", rtn);
        Assert.assertEquals("Wrong cache size", 2, count.get());
        rtn = proxy.getValidateEtagged();
        Assert.assertEquals("Wrong response", "hello1", rtn);
        Assert.assertEquals("Wrong cache size", 3, count.get());
        rtn = proxy.getValidateEtagged();
        Assert.assertEquals("Wrong response", "hello1", rtn);
        Assert.assertEquals("Wrong cache size", 4, count.get());
        client.close();
    }

    /**
     * @tpTestDetails Test for max size of client cache
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testMaxSize() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        QuarkusRestWebTarget target = client.target(generateBaseUrl());
        target.register(BrowserCacheFeature.class);
        LightweightBrowserCache cache = (LightweightBrowserCache) target.getConfiguration()
                .getProperty(BrowserCache.class.getName());
        cache.setMaxBytes(20);
        ClientCacheProxy proxy = target.proxy(ClientCacheProxy.class);

        count.set(0);

        String rtn = proxy.getCacheit("1");
        Assert.assertEquals("Wrong response", "cachecache" + 1, rtn);
        Assert.assertEquals("Wrong cache size", 1, count.get());

        rtn = proxy.getCacheit("1");
        Assert.assertEquals("Wrong response", "cachecache" + 1, rtn);
        Assert.assertEquals("Wrong cache size", 1, count.get());

        rtn = proxy.getCacheit("2");
        Assert.assertEquals("Wrong response", "cachecache" + 2, rtn);
        Assert.assertEquals("Wrong cache size", 2, count.get());

        rtn = proxy.getCacheit("2");
        Assert.assertEquals("Wrong response", "cachecache" + 2, rtn);
        Assert.assertEquals("Wrong cache size", 2, count.get());

        rtn = proxy.getCacheit("1");
        Assert.assertEquals("Wrong response", "cachecache" + 3, rtn);
        Assert.assertEquals("Wrong cache size", 3, count.get());
        client.close();
    }

    @Test
    public void testMaxSizeNoProxy() throws Exception {
        String url = PortProviderUtil.generateURL("/cache/cacheit/{id}", ClientCacheTest.class.getSimpleName());
        Client client = ClientBuilder.newClient();
        QuarkusRestWebTarget target = (QuarkusRestWebTarget) client.target(url);
        LightweightBrowserCache cache = new LightweightBrowserCache();
        cache.setMaxBytes(20);
        BrowserCacheFeature cacheFeature = new BrowserCacheFeature();
        cacheFeature.setCache(cache);
        target.register(cacheFeature);

        count.set(0);

        String rtn = target.resolveTemplate("id", "1").request().get(String.class);
        Assert.assertEquals("cachecache" + 1, rtn);
        Assert.assertEquals(1, count.get());

        rtn = target.resolveTemplate("id", "1").request().get(String.class);
        Assert.assertEquals("cachecache" + 1, rtn);
        Assert.assertEquals(1, count.get());

        rtn = target.resolveTemplate("id", "2").request().get(String.class);
        Assert.assertEquals("cachecache" + 2, rtn);
        Assert.assertEquals(2, count.get());

        rtn = target.resolveTemplate("id", "2").request().get(String.class);
        Assert.assertEquals("cachecache" + 2, rtn);
        Assert.assertEquals(2, count.get());

        rtn = target.resolveTemplate("id", "1").request().get(String.class);
        Assert.assertEquals("cachecache" + 3, rtn);
        Assert.assertEquals(3, count.get());
        client.close();
    }
}
