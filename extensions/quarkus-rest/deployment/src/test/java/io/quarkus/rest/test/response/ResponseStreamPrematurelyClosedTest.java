package io.quarkus.rest.test.response;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;

import org.apache.commons.io.IOUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.rest.test.response.resource.TestResourceImpl;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;

@DisplayName("Response Stream Prematurely Closed Test")
public class ResponseStreamPrematurelyClosedTest {

    static Client client;

    @Deployment
    public static Archive<?> deploy() throws Exception {
        WebArchive war = TestUtil.prepareArchive(ResponseStreamPrematurelyClosedTest.class.getSimpleName());
        return TestUtil.finishContainerPrepare(war, null, TestResourceImpl.class);
    }

    @BeforeAll
    public static void init() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ResponseStreamPrematurelyClosedTest.class.getSimpleName());
    }

    @Test
    @DisplayName("Test Stream")
    public void testStream() throws Exception {
        Builder builder = client.target(generateURL("/test/document/abc/content")).request();
        try (MyByteArrayOutputStream baos = new MyByteArrayOutputStream()) {
            if (!TestUtil.isIbmJdk()) {
                // builder.get().readEntity explicitly on the same line below and not saved in any temp variable
                // to let the JVM try finalizing the ClientResponse object
                InputStream ins = builder.get().readEntity(InputStream.class);
                // suggest jvm to do gc and wait the gc notification
                final CountDownLatch coutDown = new CountDownLatch(1);
                List<GarbageCollectorMXBean> gcbeans = ManagementFactory.getGarbageCollectorMXBeans();
                NotificationListener listener = new NotificationListener() {

                    public void handleNotification(Notification notification, Object handback) {
                        coutDown.countDown();
                    }
                };
                try {
                    for (GarbageCollectorMXBean gcbean : gcbeans) {
                        NotificationEmitter emitter = (NotificationEmitter) gcbean;
                        emitter.addNotificationListener(listener, null, null);
                    }
                    System.gc();
                    coutDown.await(10, TimeUnit.SECONDS);
                    IOUtils.copy(ins, baos);
                    Assertions.assertEquals(10000000, baos.size(),"Received string: " + baos.toShortString());
                } finally {
                    // remove the listener
                    for (GarbageCollectorMXBean gcbean : gcbeans) {
                        ((NotificationEmitter) gcbean).removeNotificationListener(listener);
                    }
                }
            } else {
                // workaround for Ibm jdk - doesn't allow to use NotificationEmitter with GarbageCollectorMXBean
                // builder.get().readEntity explicitly on the same line below and not saved in any temp variable
                // to let the JVM try finalizing the ClientResponse object
                IOUtils.copy(builder.get().readEntity(InputStream.class), baos);
                Assertions.assertEquals(100000000, baos.size());
            }
        }
    }

    @DisplayName("My Byte Array Output Stream")
    private static class MyByteArrayOutputStream extends ByteArrayOutputStream {

        public String getSubstring(int from, int to) {
            if (from < 0 || to > count) {
                throw new IllegalArgumentException();
            }
            return new String(buf, from, to);
        }

        public String toShortString() {
            int s = size();
            if (s <= 14000) {
                return toString();
            } else {
                return getSubstring(0, 1000) + "..." + getSubstring(s - 13000, 13000);
            }
        }
    }
}
