package io.quarkus.rest.test.core.servlet;

import static org.junit.Assert.assertEquals;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.core.servlet.resource.FilterResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Configuration
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1049
 * @tpSince RESTEasy 3.0.16
 */
public class FilterTest {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, FilterResource.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, FilterTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test for dynamic dispatching in servlet.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testDispatchDynamic() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        Invocation.Builder request = client.target(generateURL("/test/dispatch/dynamic")).request();
        Response response = request.get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals("Wrong content of response", "forward", response.readEntity(String.class));
        client.close();
    }
}
