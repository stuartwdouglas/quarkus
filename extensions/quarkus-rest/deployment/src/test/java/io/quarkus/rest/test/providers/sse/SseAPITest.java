package io.quarkus.rest.test.providers.sse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.SseEventSource;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClientBuilder;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

@DisplayName("Sse API Test")
public class SseAPITest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
            List<Class<?>> singletons = new ArrayList<Class<?>>();
            singletons.add(SseAPIImpl.class);
            return TestUtil.finishContainerPrepare(war, null, singletons, SseAPI.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, SseAPITest.class.getSimpleName());
    }

    // test for RESTEASY-2017:SSE doesn't work with inherited annotations
    @Test
    @DisplayName("Test Annotaiton Inherited")
    public void testAnnotaitonInherited() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final List<String> results = new ArrayList<String>();
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target(generateURL("/apitest/events"));
        SseEventSource msgEventSource = SseEventSource.target(target).build();
        try (SseEventSource eventSource = msgEventSource) {
            eventSource.register(event -> {
                results.add(event.readData(String.class));
                latch.countDown();
            }, ex -> {
                throw new RuntimeException(ex);
            });
            eventSource.open();
            Thread.sleep(1000);
            Client messageClient = ((QuarkusRestClientBuilder) ClientBuilder.newBuilder()).connectionPoolSize(10).build();
            WebTarget messageTarget = messageClient.target(generateURL("/apitest/send"));
            Response response = messageTarget.request().post(Entity.text("apimsg"));
            Assertions.assertEquals(204, response.getStatus());
            boolean result = latch.await(10, TimeUnit.SECONDS);
            Assertions.assertTrue(result, "Waiting for event to be delivered has timed out.");
            messageClient.close();
        }
        Assertions.assertEquals(1, results.size(), "One event message was expected.");
        Assertions.assertTrue("Expected event contains apimsg, but is:" + results.get(0), results.get(0).contains("apimsg"));
        client.close();
    }
}
