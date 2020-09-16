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

import io.quarkus.rest.test.resource.path.resource.ResourceLocatorRegexCapturingGroupSubResourceNoPath;
import io.quarkus.rest.test.resource.path.resource.ResourceLocatorRegexCapturingGroupSubResourceWithPath;
import io.quarkus.rest.test.resource.path.resource.ResourceLocatorRegexNonCapturingGroup;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @Path annotation paths can consist of Regex Non-Capturing groups used with
 *       Resource Locator scenarios.
 *
 *       User: rsearls
 *       Date: 2/18/17
 */
@DisplayName("Resource Locator Regex Non Capturing Group Test")
public class ResourceLocatorRegexNonCapturingGroupTest {

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
            // war.addAsWebInfResource(ResourceLocatorRegexNonCapturingGroupTest.class.getPackage(), "web.xml", "web.xml");
            return TestUtil.finishContainerPrepare(war, null, ResourceLocatorRegexNonCapturingGroup.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ResourceLocatorRegexNonCapturingGroupTest.class.getSimpleName());
    }

    @AfterAll
    public static void close() throws Exception {
        client.close();
    }

    @AfterAll
    public static void after() throws Exception {
    }

    @Test
    @DisplayName("Test Bird")
    public void testBird() throws Exception {
        {
            Response response = client.target(generateURL("/noCapture/nobird")).request().get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals(ERROR_MSG, "nobird success", response.readEntity(String.class));
            response.close();
        }
        {
            Response response = client.target(generateURL("/noCapture/BIRD/test")).request().get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals(ERROR_MSG, "BIRD test", response.readEntity(String.class));
            response.close();
        }
    }

    @Test
    @DisplayName("Test Fly")
    public void testFly() throws Exception {
        {
            Response response = client.target(generateURL("/noCapture/a/nofly/b")).request().get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals(ERROR_MSG, "a/nofly/b success", response.readEntity(String.class));
            response.close();
        }
        {
            Response response = client.target(generateURL("/noCapture/a/FLY/b/test")).request().get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals(ERROR_MSG, "a/FLY/b test", response.readEntity(String.class));
            response.close();
        }
    }
}
