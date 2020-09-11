package io.quarkus.rest.test.asynch;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.asynch.resource.AsynchCounterResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Asynchronous RESTEasy
 * @tpChapter Integration tests
 * @tpTestCaseDetails Tests use of SecureRandom to generate location job ids, RESTEASY-1483
 * @tpSince RESTEasy 3.1.0.Final
 */
public class AsynchCounterTest {

    static Client client;

    @BeforeClass
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterClass
    public static void close() {
        client.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    Map<String, String> contextParam = new HashMap<>();
                    contextParam.put("resteasy.async.job.service.enabled", "true");
                    contextParam.put("resteasy.secure.random.max.use", "2");
                    return TestUtil.finishContainerPrepare(war, contextParam, AsynchCounterResource.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, AsynchCounterTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test that job ids are no longer consecutive
     * @tpInfo RESTEASY-1483
     * @tpSince RESTEasy 3.1.0.Final
     */
    @Test
    public void testAsynchCounter() throws Exception {

        Response response = client.target(generateURL("?asynch=true")).request().get();
        Assert.assertEquals(Status.ACCEPTED, response.getStatus());
        String jobUrl = response.getHeaderString(HttpHeaders.LOCATION);
        int job1 = Integer.parseInt(jobUrl.substring(jobUrl.lastIndexOf('-') + 1));
        response.close();
        response = client.target(generateURL("?asynch=true")).request().get();
        Assert.assertEquals(Status.ACCEPTED, response.getStatus());
        jobUrl = response.getHeaderString(HttpHeaders.LOCATION);
        int job2 = Integer.parseInt(jobUrl.substring(jobUrl.lastIndexOf('-') + 1));
        Assert.assertTrue(job2 != job1 + 1);
        response.close();
    }
}
