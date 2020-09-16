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

import io.quarkus.rest.test.asynch.resource.LegacySuspendResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Asynchronous RESTEasy
 * @tpChapter Integration tests
 * @tpTestCaseDetails Basic asynchronous test for suspended response.
 *                    Test for org.jboss.resteasy.annotations.Suspend annotation
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Suspend Annotation Test")
public class SuspendAnnotationTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, LegacySuspendResource.class);
        }
    });

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, JaxrsAsyncTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Server is able to answer in requested time.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Positive")
    public void testPositive() throws Exception {
        Client client = ClientBuilder.newClient();
        Response response = client.target(generateURL("")).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("hello", response.readEntity(String.class), "Wrong content of response");
        response.close();
        client.close();
    }

    /**
     * @tpTestDetails Server is not able to answer in requested time.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Timeout")
    public void testTimeout() throws Exception {
        Client client = ClientBuilder.newClient();
        Response response = client.target(generateURL("/timeout")).request().get();
        Assertions.assertEquals(Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
        response.close();
        client.close();
    }
}
