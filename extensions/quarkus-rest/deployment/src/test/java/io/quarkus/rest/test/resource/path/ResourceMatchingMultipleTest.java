package io.quarkus.rest.test.resource.path;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.resource.path.resource.ResourceMatchingMultipleUserCertResource;
import io.quarkus.rest.test.resource.path.resource.ResourceMatchingMultipleUserMembershipResource;
import io.quarkus.rest.test.resource.path.resource.ResourceMatchingMultipleUserResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Resource Matching Multiple Test")
public class ResourceMatchingMultipleTest {

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ResourceMatchingMultipleTest.class.getSimpleName());
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, ResourceMatchingMultipleUserResource.class,
                    ResourceMatchingMultipleUserCertResource.class, ResourceMatchingMultipleUserMembershipResource.class);
        }
    });

    static Client client;

    @BeforeAll
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void close() {
        client.close();
    }

    /**
     * @tpTestDetails Client sends GET request for Users resource, with custom id. With 3 Resources available in the
     *                application, the correct path will be selected.
     * @tpPassCrit The correct Resource path is chosen
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Matching Users")
    public void testMatchingUsers() throws Exception {
        String answer = client.target(generateURL("/users/1")).request().get(String.class);
        Assertions.assertEquals("users/{id} 1", answer, "The incorrect resource path was chosen");
    }

    /**
     * @tpTestDetails Client sends GET request for Memberships resource, with custom id. With 3 Resources available in the
     *                application, the correct path will be selected.
     * @tpPassCrit The correct Resource path is chosen
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Matching Member Ships")
    public void testMatchingMemberShips() throws Exception {
        String answer = client.target(generateURL("/users/1/memberships")).request().get(String.class);
        Assertions.assertEquals("users/{id}/memberships 1", answer, "The incorrect resource path was chosen");
    }

    /**
     * @tpTestDetails Client sends GET request for Certs resource, with custom id. With 3 Resources available in the
     *                application, the correct path will be selected.
     * @tpPassCrit The correct Resource path is chosen
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Matching Certs")
    public void testMatchingCerts() throws Exception {
        String answer = client.target(generateURL("/users/1/certs")).request().get(String.class);
        Assertions.assertEquals("users/{id}/certs 1", answer, "The incorrect resource path was chosen");
    }
}
