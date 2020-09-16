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
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
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
public class NotFoundErrorMessageTest {
    static Client client;

    @BeforeClass
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
    }

    @AfterClass
    public static void close() {
        client.close();
    }

    private static int getWarningCount() {
        return TestUtil.getWarningCount("RESTEASY002010", false, DEFAULT_CONTAINER_QUALIFIER);
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
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
    public void testDeploy() throws IOException {
        int initWarningCount = getWarningCount();
        Response response = client.target(generateURL("/nonsence")).request().get();
        Assert.assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        response.close();

        Assert.assertEquals("Wrong count of warning messages in logs", 0, getWarningCount() - initWarningCount);
    }
}
