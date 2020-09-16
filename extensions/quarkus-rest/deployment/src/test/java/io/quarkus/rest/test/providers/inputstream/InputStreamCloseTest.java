package io.quarkus.rest.test.providers.inputstream;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.inputstream.resource.InputStreamCloseInputStream;
import io.quarkus.rest.test.providers.inputstream.resource.InputStreamCloseResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Configuration
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-741
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Input Stream Close Test")
public class InputStreamCloseTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(InputStreamCloseInputStream.class);
            return TestUtil.finishContainerPrepare(war, null, InputStreamCloseResource.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, InputStreamCloseTest.class.getSimpleName());
    }

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails New client test
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("New Client")
    public void newClient() throws Exception {
        // Resource creates and returns InputStream.
        Response response = client.target(generateURL("/create/")).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "hello");
        response.close();
        // Verify previously created InputStream has been closed.
        response = client.target(generateURL("/test/")).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
    }
}
