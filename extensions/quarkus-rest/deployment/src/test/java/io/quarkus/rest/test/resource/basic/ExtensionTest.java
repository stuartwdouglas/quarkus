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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
@DisplayName("Extension Test")
public class ExtensionTest {

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

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

    @BeforeAll
    public static void init() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Check correct values
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test It")
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
    @DisplayName("Test Error")
    public void testError() {
        Response response = client.target(PortProviderUtil.generateURL("/extension.junk", ExtensionTest.class.getSimpleName()))
                .request().get();
        Assertions.assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        response.close();
    }

    private void basicTest(String path, String body) {
        Response response = client.target(PortProviderUtil.generateURL(path, ExtensionTest.class.getSimpleName())).request()
                .get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(body, response.readEntity(String.class), "Wrong content of response");
        response.close();
    }
}
