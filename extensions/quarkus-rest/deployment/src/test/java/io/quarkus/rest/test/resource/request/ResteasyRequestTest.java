package io.quarkus.rest.test.resource.request;

import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.resource.request.resource.RequestResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpTestCaseDetails Tests for ResteasyRequest
 * @tpSince RESTEasy 4.3.2
 */
public class ResteasyRequestTest {

    static Client client;
    static WebTarget requestWebTarget;

    @BeforeClass
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
        requestWebTarget = client.target(generateURL("/request"));
    }

    @AfterClass
    public static void close() {
        client.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, RequestResource.class);
                }
            });

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ResteasyRequestTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Checks ResteasyRequest
     * @tpSince RESTEasy 4.3.2
     */
    @Test
    public void testRequest() {
        try {
            Response response = requestWebTarget.request().get();
            Assert.assertEquals(Status.OK, response.getStatus());
            final String val = response.readEntity(String.class);
            final String pattern = "^127.0.0.1/.+";
            Assert.assertTrue(String.format("Expected value '%s' to match pattern '%s'", val, pattern),
                    Pattern.matches(pattern, val));
            response.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
