package io.quarkus.rest.test.core.basic;

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
import io.quarkus.rest.test.core.basic.resource.NoApplicationSubclassResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Configuration
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for discovering root resource classes when no Application subclass is present
 * @tpSince RESTEasy 3.6.2.Final
 */
@DisplayName("No Application Subclass Test")
public class NoApplicationSubclassTest {

    private static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(NoApplicationSubclassResource.class);
            // war.addAsWebInfResource(NoApplicationSubclassTest.class.getPackage(), "NoApplicationSubclassWeb.xml",
            // "web.xml");
            return war;
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, NoApplicationSubclassTest.class.getSimpleName());
    }

    @BeforeEach
    public void setup() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Check if resource is present in application
     * @tpSince RESTEasy 3.6.2.Final
     */
    @Test
    @DisplayName("Test Resource")
    public void testResource() {
        Response response = client.target(generateURL("/myresources/hello")).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("hello world", response.readEntity(String.class), "Wrong content of response");
        response.close();
    }
}
