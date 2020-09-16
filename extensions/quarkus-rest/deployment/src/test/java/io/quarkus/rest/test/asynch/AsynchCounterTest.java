package io.quarkus.rest.test.asynch;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
@DisplayName("Asynch Counter Test")
public class AsynchCounterTest {

    static Client client;

    @BeforeAll
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void close() {
        client.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

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
    @DisplayName("Test Asynch Counter")
    public void testAsynchCounter() throws Exception {
        Response response = client.target(generateURL("?asynch=true")).request().get();
        Assertions.assertEquals(Status.ACCEPTED.getStatusCode(), response.getStatus());
        String jobUrl = response.getHeaderString(HttpHeaders.LOCATION);
        int job1 = Integer.parseInt(jobUrl.substring(jobUrl.lastIndexOf('-') + 1));
        response.close();
        response = client.target(generateURL("?asynch=true")).request().get();
        Assertions.assertEquals(Status.ACCEPTED.getStatusCode(), response.getStatus());
        jobUrl = response.getHeaderString(HttpHeaders.LOCATION);
        int job2 = Integer.parseInt(jobUrl.substring(jobUrl.lastIndexOf('-') + 1));
        Assertions.assertTrue(job2 != job1 + 1);
        response.close();
    }
}
