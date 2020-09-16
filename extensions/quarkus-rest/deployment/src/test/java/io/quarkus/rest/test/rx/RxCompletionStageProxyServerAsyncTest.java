package io.quarkus.rest.test.rx;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;

import org.jboss.resteasy.client.jaxrs.internal.CompletionStageRxInvokerProvider;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.Assert;
import io.quarkus.rest.test.rx.resource.RxCompletionStageResourceImpl;
import io.quarkus.rest.test.rx.resource.RxScheduledExecutorService;
import io.quarkus.rest.test.rx.resource.SimpleResource;
import io.quarkus.rest.test.rx.resource.TestException;
import io.quarkus.rest.test.rx.resource.TestExceptionMapper;
import io.quarkus.rest.test.rx.resource.Thing;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Reactive classes
 * @tpChapter Integration tests
 * @tpSince RESTEasy 4.0
 *
 *          These tests run synchronously on client, calling a proxy which does a synchronous invocation.
 *          The server creates and returns CompletionStages which run asynchronously.
 */
public class RxCompletionStageProxyServerAsyncTest {

    private static QuarkusRestClient client;
    private static SimpleResource proxy;

    private static List<Thing> xThingList = new ArrayList<Thing>();
    private static List<Thing> aThingList = new ArrayList<Thing>();

    static {
        for (int i = 0; i < 3; i++) {
            xThingList.add(new Thing("x"));
        }
        for (int i = 0; i < 3; i++) {
            aThingList.add(new Thing("a"));
        }
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(Thing.class);
                    war.addClass(RxScheduledExecutorService.class);
                    war.addClass(TestException.class);
                    war.setManifest(new StringAsset("Manifest-Version: 1.0\n"
                            + "Dependencies: org.jboss.resteasy.resteasy-rxjava2 services\n"));
                    return TestUtil.finishContainerPrepare(war, null, RxCompletionStageResourceImpl.class,
                            TestExceptionMapper.class);
                }
            });

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, RxCompletionStageProxyServerAsyncTest.class.getSimpleName());
    }

    //////////////////////////////////////////////////////////////////////////////
    @BeforeAll
    public static void beforeClass() throws Exception {
        client = (QuarkusRestClient) ClientBuilder.newClient();
        proxy = client.target(generateURL("/")).proxy(SimpleResource.class);
    }

    @AfterAll
    public static void after() throws Exception {
        client.close();
    }

    //////////////////////////////////////////////////////////////////////////////

    @Test
    public void testGet() throws Exception {
        String s = proxy.get();
        Assert.assertEquals("x", s);
    }

    @Test
    public void testGetThing() throws Exception {
        Thing t = proxy.getThing();
        Assert.assertEquals(new Thing("x"), t);
    }

    @Test
    public void testGetThingList() throws Exception {
        List<Thing> list = proxy.getThingList();
        Assert.assertEquals(xThingList, list);
    }

    @Test
    public void testPut() throws Exception {
        String s = proxy.put("a");
        Assert.assertEquals("a", s);
    }

    @Test
    public void testPutThing() throws Exception {
        Thing t = proxy.putThing("a");
        Assert.assertEquals(new Thing("a"), t);
    }

    @Test
    public void testPutThingList() throws Exception {
        List<Thing> list = proxy.putThingList("a");
        Assert.assertEquals(aThingList, list);
    }

    @Test
    public void testPost() throws Exception {
        String s = proxy.post("a");
        Assert.assertEquals("a", s);
    }

    @Test
    public void testPostThing() throws Exception {
        Thing t = proxy.postThing("a");
        Assert.assertEquals(new Thing("a"), t);
    }

    @Test
    public void testPostThingList() throws Exception {
        List<Thing> list = proxy.postThingList("a");
        Assert.assertEquals(aThingList, list);
    }

    @Test
    public void testDelete() throws Exception {
        String s = proxy.delete();
        Assert.assertEquals("x", s);
    }

    @Test
    public void testDeleteThing() throws Exception {
        Thing t = proxy.deleteThing();
        Assert.assertEquals(new Thing("x"), t);
    }

    @Test
    public void testDeleteThingList() throws Exception {
        List<Thing> list = proxy.deleteThingList();
        Assert.assertEquals(xThingList, list);
    }

    @Test
    public void testHead() throws Exception {
        try {
            proxy.head();
        } catch (Exception e) {
            Assert.assertTrue(throwableContains(e, "Input stream was empty, there is no entity"));
        }
    }

    @Test
    public void testOptions() throws Exception {
        String s = proxy.options();
        Assert.assertEquals("x", s);
    }

    @Test
    public void testOptionsThing() throws Exception {
        Thing t = proxy.optionsThing();
        Assert.assertEquals(new Thing("x"), t);
    }

    @Test
    public void testOptionsThingList() throws Exception {
        List<Thing> list = proxy.optionsThingList();
        Assert.assertEquals(xThingList, list);
    }

    @Test
    public void testUnhandledException() throws Exception {
        try {
            proxy.exceptionUnhandled();
            Assertions.fail("expecting Exception");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("500"));
        }
    }

    @Test
    public void testHandledException() throws Exception {
        try {
            proxy.exceptionHandled();
            Assertions.fail("expecting Exception");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("444"));
        }
    }

    @Test
    public void testGetTwoClients() throws Exception {
        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<String>();

        QuarkusRestClient client1 = (QuarkusRestClient) ClientBuilder.newClient();
        client1.register(CompletionStageRxInvokerProvider.class);
        SimpleResource proxy1 = client1.target(generateURL("/")).proxy(SimpleResource.class);
        String s1 = proxy1.get();

        QuarkusRestClient client2 = (QuarkusRestClient) ClientBuilder.newClient();
        client2.register(CompletionStageRxInvokerProvider.class);
        SimpleResource proxy2 = client2.target(generateURL("/")).proxy(SimpleResource.class);
        String s2 = proxy2.get();

        list.add(s1);
        list.add(s2);
        Assert.assertEquals(2, list.size());
        for (int i = 0; i < 2; i++) {
            Assert.assertEquals("x", list.get(i));
        }
        client1.close();
        client2.close();
    }

    @Test
    public void testGetTwoProxies() throws Exception {
        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<String>();

        SimpleResource proxy1 = client.target(generateURL("/")).proxy(SimpleResource.class);
        String s1 = proxy1.get();

        SimpleResource proxy2 = client.target(generateURL("/")).proxy(SimpleResource.class);
        String s2 = proxy2.get();

        list.add(s1);
        list.add(s2);
        Assert.assertEquals(2, list.size());
        for (int i = 0; i < 2; i++) {
            Assert.assertEquals("x", list.get(i));
        }
    }

    @Test
    public void testGetTwoCompletionStages() throws Exception {
        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<String>();

        String s1 = proxy.get();
        String s2 = proxy.get();

        list.add(s1);
        list.add(s2);
        Assert.assertEquals(2, list.size());
        for (int i = 0; i < 2; i++) {
            Assert.assertEquals("x", list.get(i));
        }
    }

    private static boolean throwableContains(Throwable t, String s) {
        while (t != null) {
            if (t.getMessage().contains(s)) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }
}
