package io.quarkus.rest.test.core.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.core.basic.resource.NoApplicationSubclassResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Configuration
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for discovering root resource classes when no Application subclass is present
 * @tpSince RESTEasy 3.6.2.Final
 */
public class NoApplicationSubclassTest {

    private static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(NoApplicationSubclassResource.class);
                    war.addAsWebInfResource(NoApplicationSubclassTest.class.getPackage(), "NoApplicationSubclassWeb.xml",
                            "web.xml");
                    return war;
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, NoApplicationSubclassTest.class.getSimpleName());
    }

    @Before
    public void setup() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Check if resource is present in application
     * @tpSince RESTEasy 3.6.2.Final
     */
    @Test
    public void testResource() {
        Response response = client.target(generateURL("/myresources/hello")).request().get();
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("Wrong content of response", "hello world", response.readEntity(String.class));
        response.close();
    }
}
