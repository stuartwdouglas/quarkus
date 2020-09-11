package io.quarkus.rest.test.providers.sse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.SseEventSource;

import org.hamcrest.CoreMatchers;
import org.jboss.logging.Logger;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.providers.sse.resource.SseSmokeMessageBodyWriter;
import io.quarkus.rest.test.providers.sse.resource.SseSmokeResource;
import io.quarkus.rest.test.providers.sse.resource.SseSmokeUser;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

public class SseSmokeTest {
    private static final Logger logger = Logger.getLogger(SseSmokeTest.class);
    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, SseSmokeMessageBodyWriter.class, SseSmokeUser.class,
                            SseSmokeResource.class);
                }
            });

    @Before
    public void init() {
        client = ClientBuilder.newBuilder().build();
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, SseSmokeTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails REST resource with SSE endpoint. Event is sent to the client.
     * @tpInfo RESTEASY-1680
     * @tpSince RESTEasy 3.5.0
     */
    @Test
    public void testSmoke() throws Exception {
        final List<String> results = new ArrayList<String>();
        WebTarget target = client.target(generateURL("/sse/events"));
        SseEventSource msgEventSource = SseEventSource.target(target).build();

        try (SseEventSource eventSource = msgEventSource) {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            eventSource.register(event -> {
                countDownLatch.countDown();
                results.add(event.readData(String.class));
            }, e -> {
                throw new RuntimeException(e);
            });
            eventSource.open();
            boolean result = countDownLatch.await(30, TimeUnit.SECONDS);
            Assert.assertTrue("Waiting for event to be delivered has timed out.", result);
        }
        Assert.assertEquals("One message was expected.", 1, results.size());
        Assert.assertThat("The message doesn't have expected content.", "Zeytin;zeytin@resteasy.org",
                CoreMatchers.is(CoreMatchers.equalTo(results.get(0))));
    }
}
