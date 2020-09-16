package io.quarkus.rest.test.resource.request;

import static io.quarkus.rest.test.ContainerConstants.DEFAULT_CONTAINER_QUALIFIER;

import java.io.IOException;
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

import io.quarkus.rest.test.core.basic.resource.DuplicateDeploymentResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Core
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.17
 * @tpTestCaseDetails Regression test for JBEAP-3725
 */
@DisplayName("Not Found Error Message Test")
public class NotFoundErrorMessageTest {

    static Client client;

    @BeforeAll
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void close() {
        client.close();
    }

    private static int getWarningCount() {
        return TestUtil.getWarningCount("RESTEASY002010", false, DEFAULT_CONTAINER_QUALIFIER);
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, DuplicateDeploymentResource.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, NotFoundErrorMessageTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Check that no ERROR message was in logs after 404.
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    @DisplayName("Test Deploy")
    public void testDeploy() throws IOException {
        int initWarningCount = getWarningCount();
        Response response = client.target(generateURL("/nonsence")).request().get();
        Assertions.assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        response.close();
        Assertions.assertEquals(0, getWarningCount() - initWarningCount, "Wrong count of warning messages in logs");
    }
}
