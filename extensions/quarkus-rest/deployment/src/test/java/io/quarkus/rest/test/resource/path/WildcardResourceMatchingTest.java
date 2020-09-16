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

import io.quarkus.rest.test.resource.path.resource.WildcardMatchingResource;
import io.quarkus.rest.test.resource.path.resource.WildcardMatchingSubResource;
import io.quarkus.rest.test.resource.path.resource.WildcardMatchingSubSubResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpTestCaseDetails Check class name of sub-resources, which process client request
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Wildcard Resource Matching Test")
public class WildcardResourceMatchingTest {

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, WildcardMatchingResource.class, WildcardMatchingSubResource.class,
                    WildcardMatchingSubSubResource.class);
        }
    });

    @BeforeAll
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void cleanup() {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, WildcardResourceMatchingTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Check root resource.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Main")
    public void testMain() {
        Response response = client.target(generateURL("/main")).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "WildcardMatchingResource");
        response.close();
    }

    /**
     * @tpTestDetails Check sub-resource.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Main Sub")
    public void testMainSub() {
        Response response = client.target(generateURL("/main/sub")).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "WildcardMatchingSubResource");
        response.close();
    }

    /**
     * @tpTestDetails Check sub-sub-resource.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Main Sub Sub")
    public void testMainSubSub() {
        Response response = client.target(generateURL("/main/sub/sub")).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "WildcardMatchingSubSubResource");
        response.close();
    }
}
