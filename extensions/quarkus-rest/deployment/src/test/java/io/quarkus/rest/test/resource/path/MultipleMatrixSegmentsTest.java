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

import io.quarkus.rest.test.resource.path.resource.MultipleMatrixSegmentsResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test that a locator and resource with same path params work
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Multiple Matrix Segments Test")
public class MultipleMatrixSegmentsTest {

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, MultipleMatrixSegmentsResource.class);
        }
    });

    @BeforeAll
    public static void init() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, MultipleMatrixSegmentsTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test segments on start and on end of path
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Multiple Start And End")
    public void testMultipleStartAndEnd() throws Exception {
        Response response = client.target(generateURL("/;name=bill;ssn=111/children/;name=skippy;ssn=3344")).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Test segments in the middle of path
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Multiple Middle")
    public void testMultipleMiddle() throws Exception {
        Response response = client.target(generateURL("/stuff/;name=first;ssn=111/;name=second;ssn=3344/first")).request()
                .get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
    }
}
