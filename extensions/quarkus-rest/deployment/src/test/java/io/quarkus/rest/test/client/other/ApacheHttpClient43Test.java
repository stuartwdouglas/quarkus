package io.quarkus.rest.test.client.other;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpAsyncClient4Engine;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.jboss.resteasy.client.jaxrs.engines.URLConnectionEngine;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestClientBuilder;
import io.quarkus.rest.test.client.other.resource.ApacheHttpClient4Resource;
import io.quarkus.rest.test.client.other.resource.ApacheHttpClient4ResourceImpl;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test connection cleanup for ApacheHttpClient4Engine and URLConnectionEngine
 * @tpSince RESTEasy 3.0.16
 */
public class ApacheHttpClient43Test {

    protected static final Logger logger = Logger.getLogger(ApacheHttpClient43Test.class.getName());

    private Class<?> engine1 = ApacheHttpClient43Engine.class;
    private Class<?> engine2 = URLConnectionEngine.class;
    private Class<?> engine3 = ApacheHttpAsyncClient4Engine.class;

    private AtomicLong counter = new AtomicLong();

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(ApacheHttpClient4Resource.class);
                    return TestUtil.finishContainerPrepare(war, null, ApacheHttpClient4ResourceImpl.class);
                }
            });

    /**
     * @tpTestDetails Create 3 threads and test GC with correct request. System.gc is called directly. Proxy is not used.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testConnectionCleanupGCBase() throws Exception {
        testConnectionCleanupGC(engine1);
        testConnectionCleanupGC(engine2);
        testConnectionCleanupGC(engine3);
    }

    protected void testConnectionCleanupGC(Class<?> engine) throws Exception {
        final Client client = createEngine(engine);
        counter.set(0);

        Thread[] threads = new Thread[3];

        for (int i = 0; i < 3; i++) {
            threads[i] = new Thread() {
                @Override
                public void run() {
                    for (int j = 0; j < 10; j++) {
                        runit(client, false);
                        System.gc();
                    }
                }
            };
        }

        for (int i = 0; i < 3; i++) {
            threads[i].start();
        }
        for (int i = 0; i < 3; i++) {
            threads[i].join();
        }

        Assert.assertEquals("Wrong count of requests", 30L, counter.get());
    }

    /**
     * @tpTestDetails Create 3 threads and test GC with correct request. System.gc is not called directly. Proxy is not used.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testConnectionCleanupAuto() throws Exception {
        testConnectionCleanupAuto(engine1);
        testConnectionCleanupAuto(engine2);
        testConnectionCleanupAuto(engine3);
    }

    protected void testConnectionCleanupAuto(Class<?> engine) throws Exception {
        final Client client = createEngine(engine);
        counter.set(0);

        Thread[] threads = new Thread[3];

        for (int i = 0; i < 3; i++) {
            threads[i] = new Thread() {
                @Override
                public void run() {
                    for (int j = 0; j < 10; j++) {
                        runit(client, true);
                    }
                }
            };
        }

        for (int i = 0; i < 3; i++) {
            threads[i].start();
        }
        for (int i = 0; i < 3; i++) {
            threads[i].join();
        }

        Assert.assertEquals("Wrong count of requests", 30L, counter.get());
    }

    /**
     * @tpTestDetails Create 3 threads and test GC with correct request. System.gc is not called directly. Proxy is used.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testConnectionCleanupProxy() throws Exception {
        testConnectionCleanupProxy(engine1);
        testConnectionCleanupProxy(engine2);
        testConnectionCleanupProxy(engine3);
    }

    protected void testConnectionCleanupProxy(Class<?> engine) throws Exception {
        final QuarkusRestClient client = createEngine(engine);
        final ApacheHttpClient4Resource proxy = client
                .target(PortProviderUtil.generateBaseUrl(ApacheHttpClient43Test.class.getSimpleName()))
                .proxy(ApacheHttpClient4Resource.class);
        counter.set(0);

        Thread[] threads = new Thread[3];

        for (int i = 0; i < 3; i++) {
            threads[i] = new Thread() {
                @Override
                public void run() {
                    for (int j = 0; j < 10; j++) {
                        String str = proxy.get();
                        Assert.assertEquals("Wrong response", "hello world", str);
                        counter.incrementAndGet();
                    }
                }
            };
        }

        for (int i = 0; i < 3; i++) {
            threads[i].start();
        }
        for (int i = 0; i < 3; i++) {
            threads[i].join();
        }

        Assert.assertEquals("Wrong count of requests", 30L, counter.get());
    }

    /**
     * @tpTestDetails Create 3 threads and test GC with incorrect request. System.gc is called directly. Proxy is not used.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testConnectionCleanupErrorGC() throws Exception {
        testConnectionCleanupErrorGC(engine1);
        testConnectionCleanupErrorGC(engine2);
        testConnectionCleanupErrorGC(engine3);
    }

    /**
     * This is regression test for RESTEASY-1273
     */
    protected void testConnectionCleanupErrorGC(Class<?> engine) throws Exception {
        final QuarkusRestClient client = createEngine(engine);
        final ApacheHttpClient4Resource proxy = client
                .target(PortProviderUtil.generateBaseUrl(ApacheHttpClient43Test.class.getSimpleName()))
                .proxy(ApacheHttpClient4Resource.class);
        counter.set(0);

        Thread[] threads = new Thread[3];

        for (int i = 0; i < 3; i++) {
            threads[i] = new Thread() {
                @Override
                public void run() {
                    for (int j = 0; j < 10; j++) {
                        callProxy(proxy);
                        System.gc();
                    }
                }
            };
        }

        for (int i = 0; i < 3; i++) {
            threads[i].start();
        }
        for (int i = 0; i < 3; i++) {
            threads[i].join();
        }

        Assert.assertEquals("Wrong count of requests", 30L, counter.get());
    }

    /**
     * @tpTestDetails Create 3 threads and test GC with incorrect request. System.gc is not called directly. Proxy is not used.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testConnectionCleanupErrorNoGC() throws Exception {
        testConnectionCleanupErrorNoGC(engine1);
        testConnectionCleanupErrorNoGC(engine2);
        testConnectionCleanupErrorNoGC(engine3);
    }

    /**
     * This is regression test for RESTEASY-1273
     */
    protected void testConnectionCleanupErrorNoGC(Class<?> engine) throws Exception {
        final QuarkusRestClient client = createEngine(engine);
        final ApacheHttpClient4Resource proxy = client
                .target(PortProviderUtil.generateBaseUrl(ApacheHttpClient43Test.class.getSimpleName()))
                .proxy(ApacheHttpClient4Resource.class);
        counter.set(0);

        Thread[] threads = new Thread[3];

        for (int i = 0; i < 3; i++) {
            threads[i] = new Thread() {
                @Override
                public void run() {
                    for (int j = 0; j < 10; j++) {
                        try {
                            proxy.error();
                        } catch (NotFoundException e) {
                            Assert.assertEquals(e.getResponse().getStatus(), 404);
                            e.getResponse().close();
                            counter.incrementAndGet();
                        }
                    }
                }
            };
        }

        for (int i = 0; i < 3; i++) {
            threads[i].start();
        }
        for (int i = 0; i < 3; i++) {
            threads[i].join();
        }

        Assert.assertEquals("Wrong count of requests", 30L, counter.get());
    }

    /**
     * @tpTestDetails Create 3 threads and test GC with incorrect request. System.gc is not called directly.
     *                Proxy is used. Data is sent during request.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testConnectionWithRequestBody() throws InterruptedException {
        testConnectionWithRequestBody(engine1);
        testConnectionWithRequestBody(engine2);
        testConnectionWithRequestBody(engine3);
    }

    protected void testConnectionWithRequestBody(Class<?> engine) throws InterruptedException {
        final QuarkusRestClient client = createEngine(engine);
        final ApacheHttpClient4Resource proxy = client
                .target(PortProviderUtil.generateBaseUrl(ApacheHttpClient43Test.class.getSimpleName()))
                .proxy(ApacheHttpClient4Resource.class);
        counter.set(0);

        Thread[] threads = new Thread[3];

        for (int i = 0; i < 3; i++) {
            threads[i] = new Thread() {
                @Override
                public void run() {
                    for (int j = 0; j < 10; j++) {
                        String res = proxy.getData(String.valueOf(j));
                        Assert.assertNotNull("Response should not be null", res);
                        counter.incrementAndGet();
                    }
                }
            };
        }

        for (int i = 0; i < 3; i++) {
            threads[i].start();
        }
        for (int i = 0; i < 3; i++) {
            threads[i].join();
        }

        Assert.assertEquals("Wrong count of requests", 30L, counter.get());
    }

    private void callProxy(ApacheHttpClient4Resource proxy) {
        try {
            proxy.error();
        } catch (NotFoundException e) {
            Assert.assertEquals(e.getResponse().getStatus(), 404);
            counter.incrementAndGet();
        }
    }

    private QuarkusRestClient createEngine(Class<?> engine) {
        RequestConfig reqConfig = RequestConfig.custom() // apache HttpClient specific
                .setConnectTimeout(5000)
                .setSocketTimeout(5000)
                .setConnectionRequestTimeout(5000)
                .build();
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(reqConfig)
                .setMaxConnTotal(3)
                .build();

        final ClientHttpEngine executor;

        if (engine.isAssignableFrom(ApacheHttpClient43Engine.class)) {
            executor = new ApacheHttpClient43Engine(httpClient);
        } else if (engine.isAssignableFrom(ApacheHttpAsyncClient4Engine.class)) {
            CloseableHttpAsyncClient client = HttpAsyncClientBuilder.create().setMaxConnTotal(3).build();
            executor = new ApacheHttpAsyncClient4Engine(client, true);
        } else if (engine.isAssignableFrom(URLConnectionEngine.class)) {
            executor = new URLConnectionEngine();
        } else {
            Assert.fail("unknown engine");
            executor = null;
        }

        QuarkusRestClient client = ((QuarkusRestClientBuilder) ClientBuilder.newBuilder()).httpEngine(executor).build();
        return client;
    }

    private void runit(Client client, boolean release) {
        WebTarget target = client
                .target(PortProviderUtil.generateBaseUrl(ApacheHttpClient43Test.class.getSimpleName() + "/test"));
        try {
            Response response = target.request().get();
            Assert.assertEquals(Status.OK, response.getStatus());
            Assert.assertEquals("Wrong response", "hello world", response.readEntity(String.class));
            if (release) {
                response.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        counter.incrementAndGet();
    }
}
