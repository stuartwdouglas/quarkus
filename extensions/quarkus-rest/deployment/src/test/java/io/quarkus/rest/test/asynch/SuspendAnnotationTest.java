package io.quarkus.rest.test.asynch;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
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
public class SuspendAnnotationTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
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
    public void testPositive() throws Exception {
        Client client = ClientBuilder.newClient();
        Response response = client.target(generateURL("")).request().get();

        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("Wrong content of response", "hello", response.readEntity(String.class));

        response.close();
        client.close();
    }

    /**
     * @tpTestDetails Server is not able to answer in requested time.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testTimeout() throws Exception {
        Client client = ClientBuilder.newClient();
        Response response = client.target(generateURL("/timeout")).request().get();

        Assert.assertEquals(Status.SERVICE_UNAVAILABLE, response.getStatus());

        response.close();
        client.close();
    }
}
