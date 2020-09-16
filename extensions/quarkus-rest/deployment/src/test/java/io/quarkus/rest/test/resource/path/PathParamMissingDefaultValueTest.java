package io.quarkus.rest.test.resource.path;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.resource.path.resource.PathParamMissingDefaultValueBeanParamEntity;
import io.quarkus.rest.test.resource.path.resource.PathParamMissingDefaultValueResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpSince RESTEasy 4.0.0
 * @tpTestCaseDetails Check for slash in URL
 */
@DisplayName("Path Param Missing Default Value Test")
public class PathParamMissingDefaultValueTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(PathParamMissingDefaultValueBeanParamEntity.class);
            return TestUtil.finishContainerPrepare(war, null, PathParamMissingDefaultValueResource.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, PathParamMissingDefaultValueTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Missing @PathParam in BeanParam with no @DefaultValue should get java default value.
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    @DisplayName("Test Trailing Slash")
    public void testTrailingSlash() throws Exception {
        Client client = ClientBuilder.newClient();
        Response response = client.target(generateURL("/resource/test/")).request().get();
        assertEquals(200, response.getStatus());
        assertEquals("nullnullnullnullnullnull", response.readEntity(String.class), "Wrong response");
        client.close();
    }
}
