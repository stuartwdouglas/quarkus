package io.quarkus.rest.test.resource.path;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
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

import io.quarkus.rest.test.resource.path.resource.ResourceLocatorRegexCapturingGroup;
import io.quarkus.rest.test.resource.path.resource.ResourceLocatorRegexCapturingGroupSubResourceNoPath;
import io.quarkus.rest.test.resource.path.resource.ResourceLocatorRegexCapturingGroupSubResourceWithPath;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resources
 * @tpChapter Integration tests
 * @tpTestCaseDetails @Path annotation paths can consist of Regex Capturing groups used with
 *                    Resource Locator scenarios.
 * @tpSince RESTEasy 3.0.22
 *
 *          User: rsearls
 *          Date: 2/17/17
 */
@DisplayName("Resource Locator Regex Capturing Group Test")
public class ResourceLocatorRegexCapturingGroupTest {

    private static final String ERROR_MSG = "Response contain wrong content";

    static Client client;

    @BeforeAll
    public static void setup() throws Exception {
        client = ClientBuilder.newClient();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(ResourceLocatorRegexCapturingGroupSubResourceNoPath.class,
                    ResourceLocatorRegexCapturingGroupSubResourceWithPath.class);
            // war.addAsWebInfResource(ResourceLocatorRegexCapturingGroupTest.class.getPackage(), "web.xml", "web.xml");
            return TestUtil.finishContainerPrepare(war, null, ResourceLocatorRegexCapturingGroup.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ResourceLocatorRegexCapturingGroupTest.class.getSimpleName());
    }

    @AfterAll
    public static void close() throws Exception {
        client.close();
    }

    @AfterAll
    public static void after() throws Exception {
    }

    /**
     * @tpTestDetails Test for root resource and for subresource.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Basic")
    public void testBasic() throws Exception {
        {
            Response response = client.target(generateURL("/capture/basic")).request().get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals(ERROR_MSG, "basic success", response.readEntity(String.class));
            response.close();
        }
        {
            Response response = client.target(generateURL("/capture/BASIC/test")).request().get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals(ERROR_MSG, "BASIC test", response.readEntity(String.class));
            response.close();
        }
    }

    @Test
    @DisplayName("Test Bird")
    public void testBird() throws Exception {
        {
            Response response = client.target(generateURL("/capture/nobird")).request().get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals(ERROR_MSG, "nobird success", response.readEntity(String.class));
            response.close();
        }
        {
            Response response = client.target(generateURL("/capture/BIRD/test")).request().get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals(ERROR_MSG, "BIRD test", response.readEntity(String.class));
            response.close();
        }
    }

    @Test
    @DisplayName("Test Fly")
    public void testFly() throws Exception {
        {
            Response response = client.target(generateURL("/capture/a/nofly/b")).request().get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals(ERROR_MSG, "a/nofly/b success", response.readEntity(String.class));
            response.close();
        }
        {
            Response response = client.target(generateURL("/capture/a/FLY/b/test")).request().get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals(ERROR_MSG, "a/FLY/b test", response.readEntity(String.class));
            response.close();
        }
    }
}
