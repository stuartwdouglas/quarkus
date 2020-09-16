package io.quarkus.rest.test.client;

import java.io.UnsupportedEncodingException;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.client.resource.AbortMessageResourceFilter;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpTestCaseDetails RESTEASY-1540
 * @tpSince RESTEasy 3.1.0.Final
 */
public class AbortMessageTest {
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
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, AbortMessageResourceFilter.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, AbortMessageTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Send response with "Aborted"
     * @tpSince RESTEasy 3.1.0.Final
     */
    @Test
    public void testAbort() throws UnsupportedEncodingException {
        WebTarget target = client.target(generateURL("/showproblem"));
        Response response = target.request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("aborted", response.readEntity(String.class));
    }
}
