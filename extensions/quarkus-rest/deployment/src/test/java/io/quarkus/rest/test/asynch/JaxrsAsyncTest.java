package io.quarkus.rest.test.asynch;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.asynch.resource.JaxrsAsyncResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Asynchronous RESTEasy
 * @tpChapter Integration tests
 * @tpTestCaseDetails Basic asynchronous test. Resource creates new threads.
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Jaxrs Async Test")
public class JaxrsAsyncTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, JaxrsAsyncResource.class);
        }
    });

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, JaxrsAsyncTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Correct response excepted.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Success")
    public void testSuccess() throws Exception {
        Client client = ClientBuilder.newClient();
        Response response = client.target(generateURL("")).request().get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("hello", response.readEntity(String.class), "Wrong response");
        response.close();
        client.close();
    }

    /**
     * @tpTestDetails Timeout exception should be thrown.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Timeout")
    public void testTimeout() throws Exception {
        Client client = ClientBuilder.newClient();
        Response response = client.target(generateURL("/timeout")).request().get();
        Assertions.assertEquals(503, response.getStatus());
        response.close();
        client.close();
    }

    /**
     * @tpTestDetails Negative timeout value is set to response in end-point. Regression test for JBEAP-4695.
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    @DisplayName("Test Negative Timeout")
    public void testNegativeTimeout() throws Exception {
        Client client = ClientBuilder.newClient();
        Response response = client.target(generateURL("/negative")).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("hello", response.readEntity(String.class), "Wrong response");
        response.close();
        client.close();
    }

    /**
     * @tpTestDetails Zero timeout value is set to response in end-point. Regression test for JBEAP-4695.
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    @DisplayName("Test Zero Timeout")
    public void testZeroTimeout() throws Exception {
        Client client = ClientBuilder.newClient();
        Response response = client.target(generateURL("/zero")).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("hello", response.readEntity(String.class), "Wrong response");
        response.close();
        client.close();
    }
}
