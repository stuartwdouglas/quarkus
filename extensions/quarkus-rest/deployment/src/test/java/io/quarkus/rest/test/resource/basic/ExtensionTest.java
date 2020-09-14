package io.quarkus.rest.test.resource.basic;

import java.util.HashMap;
import java.util.Map;
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

import io.quarkus.rest.test.resource.basic.resource.ExtensionResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for resteasy.media.type.mappings and resteasy.language.mappings parameters
 * @tpSince RESTEasy 3.0.16
 */
public class ExtensionTest {

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    Map<String, String> params = new HashMap<>();
                    params.put("resteasy.media.type.mappings", "xml : application/xml, html : text/html, txt : text/plain");
                    params.put("resteasy.language.mappings", "en : en-US");
                    return TestUtil.finishContainerPrepare(war, params, ExtensionResource.class);
                }
            });

    @BeforeClass
    public static void init() {
        client = ClientBuilder.newClient();
    }

    @AfterClass
    public static void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Check correct values
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testIt() {
        basicTest("/extension.xml", "xml");
        basicTest("/extension.html.en", "html");
        basicTest("/extension.en.html", "html");
        basicTest("/extension/stuff.old.en.txt", "plain");
        basicTest("/extension/stuff.en.old.txt", "plain");
        basicTest("/extension/stuff.en.txt.old", "plain");
    }

    /**
     * @tpTestDetails Check wrong value
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testError() {
        Response response = client.target(PortProviderUtil.generateURL("/extension.junk", ExtensionTest.class.getSimpleName()))
                .request().get();
        Assert.assertEquals(Status.NOT_FOUND, response.getStatus());
        response.close();
    }

    private void basicTest(String path, String body) {
        Response response = client.target(PortProviderUtil.generateURL(path, ExtensionTest.class.getSimpleName())).request()
                .get();
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("Wrong content of response", body, response.readEntity(String.class));
        response.close();
    }
}
