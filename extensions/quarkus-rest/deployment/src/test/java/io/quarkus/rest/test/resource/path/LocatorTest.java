package io.quarkus.rest.test.resource.path;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.resource.path.resource.LocatorResource;
import io.quarkus.rest.test.resource.path.resource.LocatorTestLocator;
import io.quarkus.rest.test.resource.path.resource.LocatorTestLocator2;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Locator Test")
public class LocatorTest {

    static Client client;

    @BeforeAll
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void close() {
        client.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, LocatorResource.class, LocatorTestLocator.class,
                    LocatorTestLocator2.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, LocatorTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Client sends GET request for the resource which is not annotated with any HTTP method anotation.
     *                This resource created new object resource and passes the request to this new created object.
     * @tpPassCrit Correct response is returned from the server
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Locator With Sub With Path Annotation")
    public void testLocatorWithSubWithPathAnnotation() {
        Response response = client.target(generateURL("/locator/responseok/responseok")).request().get();
        Assertions.assertEquals(200, response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Client sends GET request with the path which matches multiple resources.
     * @tpPassCrit Correct resource is used and successful response is returned.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Multiple Path Options")
    public void testMultiplePathOptions() {
        Response response = client.target(generateURL("/resource/responseok")).request().options();
        Assertions.assertEquals(200, response.getStatus());
        response.close();
    }
}
