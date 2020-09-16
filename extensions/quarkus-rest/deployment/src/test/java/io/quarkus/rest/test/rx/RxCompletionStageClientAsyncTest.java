package io.quarkus.rest.test.rx;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.internal.CompletionStageRxInvokerProvider;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestClientBuilder;
import io.quarkus.rest.test.rx.resource.RxScheduledExecutorService;
import io.quarkus.rest.test.rx.resource.SimpleResourceImpl;
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
 *          These tests run asynchronously on client, calling a CompletionStageRxInvoker.
 *          The server creates and returns objects synchronously.
 */
public class RxCompletionStageClientAsyncTest {

    private static QuarkusRestClient client;

    private static List<Thing> xThingList = new ArrayList<Thing>();
    private static List<Thing> aThingList = new ArrayList<Thing>();
    private static Entity<String> aEntity = Entity.entity("a", MediaType.TEXT_PLAIN_TYPE);
    private static GenericType<List<Thing>> LIST_OF_THING = new GenericType<List<Thing>>() {
    };

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
                    return TestUtil.finishContainerPrepare(war, null, SimpleResourceImpl.class, TestExceptionMapper.class);
                }
            });

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, RxCompletionStageClientAsyncTest.class.getSimpleName());
    }

    //////////////////////////////////////////////////////////////////////////////
    @BeforeAll
    public static void beforeClass() throws Exception {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterAll
    public static void after() throws Exception {
        client.close();
    }

    //////////////////////////////////////////////////////////////////////////////
    @Test
    public void testGet() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/get/string")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<Response> completionStage = invoker.get();
        Assert.assertEquals("x", completionStage.toCompletableFuture().get().readEntity(String.class));
    }

    @Test
    public void testGetString() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/get/string")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<String> completionStage = invoker.get(String.class);
        Assert.assertEquals("x", completionStage.toCompletableFuture().get());
    }

    @Test
    public void testGetThing() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/get/thing")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<Thing> completionStage = invoker.get(Thing.class);
        Assert.assertEquals(new Thing("x"), completionStage.toCompletableFuture().get());
    }

    @Test
    public void testGetThingList() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/get/thing/list")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<List<Thing>> completionStage = invoker.get(LIST_OF_THING);
        Assert.assertEquals(xThingList, completionStage.toCompletableFuture().get());
    }

    @Test
    public void testPut() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/put/string")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<Response> completionStage = invoker.put(aEntity);
        Assert.assertEquals("a", completionStage.toCompletableFuture().get().readEntity(String.class));
    }

    @Test
    public void testPutThing() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/put/thing")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<Thing> completionStage = invoker.put(aEntity, Thing.class);
        Assert.assertEquals(new Thing("a"), completionStage.toCompletableFuture().get());
    }

    @Test
    public void testPutThingList() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/put/thing/list")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<List<Thing>> completionStage = invoker.put(aEntity, LIST_OF_THING);
        Assert.assertEquals(aThingList, completionStage.toCompletableFuture().get());
    }

    @Test
    public void testPost() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/post/string")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<Response> completionStage = invoker.post(aEntity);
        Assert.assertEquals("a", completionStage.toCompletableFuture().get().readEntity(String.class));
    }

    @Test
    public void testPostThing() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/post/thing")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<Thing> completionStage = invoker.post(aEntity, Thing.class);
        Assert.assertEquals(new Thing("a"), completionStage.toCompletableFuture().get());
    }

    @Test
    public void testPostThingList() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/post/thing/list")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<List<Thing>> completionStage = invoker.post(aEntity, LIST_OF_THING);
        Assert.assertEquals(aThingList, completionStage.toCompletableFuture().get());
    }

    @Test
    public void testDelete() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/delete/string")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<Response> completionStage = invoker.delete();
        Assert.assertEquals("x", completionStage.toCompletableFuture().get().readEntity(String.class));
    }

    @Test
    public void testDeleteThing() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/delete/thing")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<Thing> completionStage = invoker.delete(Thing.class);
        Assert.assertEquals(new Thing("x"), completionStage.toCompletableFuture().get());
    }

    @Test
    public void testDeleteThingList() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/delete/thing/list")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<List<Thing>> completionStage = invoker.delete(LIST_OF_THING);
        Assert.assertEquals(xThingList, completionStage.toCompletableFuture().get());
    }

    @Test
    public void testHead() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/head/string")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<Response> completionStage = invoker.head();
        Response response = completionStage.toCompletableFuture().get();
        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void testOptions() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/options/string")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<Response> completionStage = invoker.options();
        Assert.assertEquals("x", completionStage.toCompletableFuture().get().readEntity(String.class));
    }

    @Test
    public void testOptionsThing() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/options/thing")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<Thing> completionStage = invoker.options(Thing.class);
        Assert.assertEquals(new Thing("x"), completionStage.toCompletableFuture().get());
    }

    @Test
    public void testOptionsThingList() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/options/thing/list")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<List<Thing>> completionStage = invoker.options(LIST_OF_THING);
        Assert.assertEquals(xThingList, completionStage.toCompletableFuture().get());
    }

    @Test
    @Disabled // TRACE is disabled by default in Wildfly
    public void testTrace() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/trace/string")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<Response> completionStage = invoker.trace();
        Assert.assertEquals("x", completionStage.toCompletableFuture().get().readEntity(String.class));
    }

    @Test
    @Disabled // TRACE is disabled by default in Wildfly
    public void testTraceThing() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/trace/thing")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<Thing> completionStage = invoker.trace(Thing.class);
        Assert.assertEquals(new Thing("x"), completionStage.toCompletableFuture().get());
    }

    @Test
    @Disabled // TRACE is disabled by default in Wildfly
    public void testTraceThingList() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/trace/thing/list")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<List<Thing>> completionStage = invoker.trace(LIST_OF_THING);
        Assert.assertEquals(xThingList, completionStage.toCompletableFuture().get());
    }

    @Test
    public void testMethodGet() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/get/string")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<Response> completionStage = invoker.method("GET");
        Assert.assertEquals("x", completionStage.toCompletableFuture().get().readEntity(String.class));
    }

    @Test
    public void testMethodGetThing() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/get/thing")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<Thing> completionStage = invoker.method("GET", Thing.class);
        Assert.assertEquals(new Thing("x"), completionStage.toCompletableFuture().get());
    }

    @Test
    public void testMethodGetThingList() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/get/thing/list")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<List<Thing>> completionStage = invoker.method("GET", LIST_OF_THING);
        Assert.assertEquals(xThingList, completionStage.toCompletableFuture().get());
    }

    @Test
    public void testMethodPost() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/post/string")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<Response> completionStage = invoker.method("POST", aEntity);
        Assert.assertEquals("a", completionStage.toCompletableFuture().get().readEntity(String.class));
    }

    @Test
    public void testMethodPostThing() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/post/thing")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<Thing> completionStage = invoker.method("POST", aEntity, Thing.class);
        Assert.assertEquals(new Thing("a"), completionStage.toCompletableFuture().get());
    }

    @Test
    public void testMethodPostThingList() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/post/thing/list")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<List<Thing>> completionStage = invoker.method("POST", aEntity, LIST_OF_THING);
        Assert.assertEquals(aThingList, completionStage.toCompletableFuture().get());
    }

    @Test
    public void testScheduledExecutorService() throws Exception {
        {
            RxScheduledExecutorService.used = false;
            CompletionStageRxInvoker invoker = client.target(generateURL("/get/string")).request()
                    .rx(CompletionStageRxInvoker.class);
            CompletionStage<Response> completionStage = invoker.get();
            Assert.assertFalse(RxScheduledExecutorService.used);
            Assert.assertEquals("x", completionStage.toCompletableFuture().get().readEntity(String.class));
        }

        {
            RxScheduledExecutorService.used = false;
            RxScheduledExecutorService executor = new RxScheduledExecutorService();
            QuarkusRestClient client = ((QuarkusRestClientBuilder) ClientBuilder.newBuilder()).executorService(executor)
                    .build();
            client.register(CompletionStageRxInvokerProvider.class);
            CompletionStageRxInvoker invoker = client.target(generateURL("/get/string")).request()
                    .rx(CompletionStageRxInvoker.class);
            CompletionStage<Response> completionStage = invoker.get();
            Assert.assertTrue(RxScheduledExecutorService.used);
            Assert.assertEquals("x", completionStage.toCompletableFuture().get().readEntity(String.class));
            client.close();
        }
    }

    @Test
    public void testUnhandledException() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/exception/unhandled")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<Thing> completionStage = (CompletionStage<Thing>) invoker.get(Thing.class);
        AtomicReference<Throwable> value = new AtomicReference<Throwable>();
        CountDownLatch latch = new CountDownLatch(1);
        completionStage.whenComplete((Thing t1, Throwable t2) -> {
            value.set(t2);
            latch.countDown();
        });
        boolean waitResult = latch.await(30, TimeUnit.SECONDS);
        Assert.assertTrue("Waiting for event to be delivered has timed out.", waitResult);
        Assert.assertTrue(value.get().getMessage().contains("500"));
    }

    @Test
    public void testHandledException() throws Exception {
        CompletionStageRxInvoker invoker = client.target(generateURL("/exception/handled")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<Thing> completionStage = (CompletionStage<Thing>) invoker.get(Thing.class);
        AtomicReference<Throwable> value = new AtomicReference<Throwable>();
        CountDownLatch latch = new CountDownLatch(1);
        completionStage.whenComplete((Thing t1, Throwable t2) -> {
            value.set(t2);
            latch.countDown();
        });
        boolean waitResult = latch.await(30, TimeUnit.SECONDS);
        Assert.assertTrue("Waiting for event to be delivered has timed out.", waitResult);
        Assert.assertTrue(value.get().getMessage().contains("444"));
    }

    @Test
    public void testGetTwoClients() throws Exception {
        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<String>();

        QuarkusRestClient client1 = (QuarkusRestClient) ClientBuilder.newClient();
        client1.register(CompletionStageRxInvokerProvider.class);
        CompletionStageRxInvoker invoker1 = client1.target(generateURL("/get/string")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<Response> completionStage1 = (CompletionStage<Response>) invoker1.get();

        QuarkusRestClient client2 = (QuarkusRestClient) ClientBuilder.newClient();
        client2.register(CompletionStageRxInvokerProvider.class);
        CompletionStageRxInvoker invoker2 = client2.target(generateURL("/get/string")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<Response> completionStage2 = (CompletionStage<Response>) invoker2.get();

        list.add(completionStage1.toCompletableFuture().get().readEntity(String.class));
        list.add(completionStage2.toCompletableFuture().get().readEntity(String.class));

        Assert.assertEquals(2, list.size());
        for (int i = 0; i < 2; i++) {
            Assert.assertEquals("x", list.get(i));
        }
        client1.close();
        client2.close();
    }

    @Test
    public void testGetTwoInvokers() throws Exception {
        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<String>();

        CompletionStageRxInvoker invoker1 = client.target(generateURL("/get/string")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<Response> completionStage1 = (CompletionStage<Response>) invoker1.get();

        CompletionStageRxInvoker invoker2 = client.target(generateURL("/get/string")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<Response> completionStage2 = (CompletionStage<Response>) invoker2.get();

        list.add(completionStage1.toCompletableFuture().get().readEntity(String.class));
        list.add(completionStage2.toCompletableFuture().get().readEntity(String.class));

        Assert.assertEquals(2, list.size());
        for (int i = 0; i < 2; i++) {
            Assert.assertEquals("x", list.get(i));
        }
    }

    @Test
    public void testGetTwoCompletionStages() throws Exception {
        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<String>();

        CompletionStageRxInvoker invoker = client.target(generateURL("/get/string")).request()
                .rx(CompletionStageRxInvoker.class);
        CompletionStage<Response> completionStage1 = (CompletionStage<Response>) invoker.get();
        CompletionStage<Response> completionStage2 = (CompletionStage<Response>) invoker.get();

        list.add(completionStage1.toCompletableFuture().get().readEntity(String.class));
        list.add(completionStage2.toCompletableFuture().get().readEntity(String.class));

        Assert.assertEquals(2, list.size());
        for (int i = 0; i < 2; i++) {
            Assert.assertEquals("x", list.get(i));
        }
    }
}
