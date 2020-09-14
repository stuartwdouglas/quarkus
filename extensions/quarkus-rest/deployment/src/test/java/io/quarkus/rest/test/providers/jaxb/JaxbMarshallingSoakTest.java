package io.quarkus.rest.test.providers.jaxb;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.jboss.logging.Logger;
import org.jboss.resteasy.utils.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClientBuilder;
import io.quarkus.rest.test.providers.jaxb.resource.JaxbMarshallingSoakAsyncService;
import io.quarkus.rest.test.providers.jaxb.resource.JaxbMarshallingSoakItem;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails JAXB shouldn't have a concurrent problem and should unmarshall a Map property all the time
 * @tpSince RESTEasy 3.0.16
 */

public class JaxbMarshallingSoakTest {
    private static Logger logger = Logger.getLogger(JaxbMarshallingSoakTest.class);
    public static int iterator = 500;
    public static AtomicInteger counter = new AtomicInteger();
    public static CountDownLatch latch;
    public static JAXBContext ctx;
    public static String itemString;
    int timeout = TimeoutUtil.adjust(60);

    static Client client;

    @Before
    public void init() {
        client = ((QuarkusRestClientBuilder) ClientBuilder.newBuilder())
                .connectTimeout(5000, TimeUnit.MILLISECONDS)
                .connectionCheckoutTimeout(5000, TimeUnit.MILLISECONDS)
                .readTimeout(5000, TimeUnit.MILLISECONDS)
                .maxPooledPerRoute(500)
                .build();
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(JaxbMarshallingSoakItem.class, TestUtil.class, PortProviderUtil.class, TimeoutUtil.class);
                    Map<String, String> contextParam = new HashMap<>();
                    contextParam.put("resteasy.async.job.service.enabled", "true");
                    // Arquillian in the deployment use if TimeoutUtil in the deployment

                    return TestUtil.finishContainerPrepare(war, contextParam, JaxbMarshallingSoakAsyncService.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, JaxbMarshallingSoakTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test with client.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void basicTest() throws Exception {
        latch = new CountDownLatch(iterator);
        ctx = JAXBContext.newInstance(JaxbMarshallingSoakItem.class);
        counter.set(0);
        itemString = setString();
        logger.info(String.format("Request: %s", itemString));
        for (int i = 0; i < iterator; i++) {
            WebTarget target = client.target(generateURL("/mpac/add?oneway=true"));
            Response response = target.request().post(Entity.entity(itemString, "application/xml"));
            Assert.assertEquals(Status.ACCEPTED, response.getStatus());
            response.close();
        }
        latch.await(10, TimeUnit.SECONDS);
        String message = String.format(new StringBuilder().append("RESTEasy should successes with marshalling %d times.")
                .append("But RESTEasy successes only %d times.").toString(), iterator, counter.get());
        Assert.assertEquals(message, iterator, counter.get());
    }

    /**
     * @tpTestDetails Server test.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void compare() throws Exception {
        itemString = setString();
        ctx = JAXBContext.newInstance(JaxbMarshallingSoakItem.class);

        counter.set(0);

        Thread[] threads = new Thread[iterator];
        for (int i = 0; i < iterator; i++) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    byte[] bytes = itemString.getBytes();
                    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                    JaxbMarshallingSoakItem item = null;
                    try {
                        item = (JaxbMarshallingSoakItem) ctx.createUnmarshaller().unmarshal(bais);
                    } catch (JAXBException e) {
                        throw new RuntimeException(e);
                    }
                    item.toString();
                    counter.incrementAndGet();

                }
            };
            threads[i] = thread;
        }
        ExecutorService threadPool = Executors.newFixedThreadPool(100);
        for (int i = 0; i < iterator; i++) {
            threadPool.submit(threads[i]);
        }
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(timeout, TimeUnit.SECONDS)) {
                Assert.fail(String.format("Clients did not terminate in %s seconds", timeout));
            }
        } catch (InterruptedException e) {
            Assert.fail("ExecutorService[threadPool] was interrupted");
        }
        String message = String.format(new StringBuilder().append("RESTEasy should successes with marshalling %d times.")
                .append("But RESTEasy successes only %d times.").toString(), iterator, counter.get());
        Assert.assertEquals(message, iterator, counter.get());
    }

    private String setString() {
        StringBuffer sbuffer = new StringBuffer();
        sbuffer.append("<item>");
        sbuffer.append("<price>1000</price>");
        sbuffer.append("<description>Allah Hafiz</description>");
        sbuffer.append("<requestID>");
        sbuffer.append("i");
        sbuffer.append("</requestID>");

        sbuffer.append("<dummy1>DUMMY1</dummy1>");
        sbuffer.append("<dummy2>DUMMY2</dummy2>");
        sbuffer.append("<dummy3>DUMMY3</dummy3>");
        sbuffer.append("<dummy4>DUMMY4</dummy4>");
        sbuffer.append("<dummy5>DUMMY5</dummy5>");
        sbuffer.append("<dummy6>DUMMY6</dummy6>");
        sbuffer.append("<dummy7>DUMMY7</dummy7>");
        sbuffer.append("<dummy8>DUMMY8</dummy8>");

        sbuffer.append("<harness>");
        sbuffer.append("<entry>");
        sbuffer.append("<key>P_REGIONCD</key>");
        sbuffer.append("<value>325</value>");
        sbuffer.append("</entry>");
        sbuffer.append("<entry>");
        sbuffer.append("<key>P_COUNTYMUN</key>");
        sbuffer.append("<value>447</value>");
        sbuffer.append("</entry>");
        sbuffer.append("<entry>f");
        sbuffer.append("<key>p_SrcView</key>");
        sbuffer.append("<value>C</value>");
        sbuffer.append("</entry>");
        sbuffer.append("</harness>");

        sbuffer.append("</item>");
        return sbuffer.toString();
    }
}
