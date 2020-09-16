package io.quarkus.rest.test.response;

import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.response.resource.CookieResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Cookie Test")
public class CookieTest {

    protected final Logger logger = Logger.getLogger(VariantsTest.class.getName());

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, CookieResource.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, CookieTest.class.getSimpleName());
    }

    @BeforeAll
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void close() {
        client.close();
        client = null;
    }

    /**
     * @tpTestDetails Client sends GET request to the server, server text response, the response is then checked, that
     *                it contains cookie with guid parameter sent by server.
     * @tpPassCrit Response contains cookie sent by server
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Weird Cookie")
    public void testWeirdCookie() {
        Response response = client.target(generateURL("/cookie/weird")).request().get();
        Assertions.assertEquals(response.getStatus(), 200);
        Map<String, NewCookie> cookies = response.getCookies();
        // for (Map.Entry<String, NewCookie> cookieEntry : cookies.entrySet()) {
        // logger.debug("[" + cookieEntry.getKey() + "] >>>>>> " + cookieEntry.getValue() + "");
        // }
        // for (Map.Entry<String, List<String>> headerEntry : response.getStringHeaders().entrySet()) {
        // logger.debug("header: " + headerEntry.getKey());
        // for (String val : headerEntry.getValue()) {
        // logger.debug("    " + val);
        // }
        // }
        Assertions.assertTrue(cookies.containsKey("guid"),
                "Cookie in the response doesn't contain 'guid', which was in the cookie send by the server.");
        response.close();
    }

    /**
     * @tpTestDetails Client sends GET request to the server, server text response, the response is then checked, that
     *                it contains standard cookie sent by server.
     * @tpPassCrit Response contains cookie sent by server
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Standard Cookie")
    public void testStandardCookie() {
        Response response = client.target(generateURL("/cookie/standard")).request().get();
        Assertions.assertEquals(response.getStatus(), 200);
        for (Map.Entry<String, NewCookie> cookieEntry : response.getCookies().entrySet()) {
            // logger.debug("[" + cookieEntry.getKey() + "] >>>>>> " + cookieEntry.getValue() + "");
            Assertions.assertEquals("UserID=JohnDoe;Version=1;Max-Age=3600", cookieEntry.getValue().toString(),
                    "Cookie content in the response doesn't match the cookie content send by the server.");
        }
        // for (Map.Entry<String, List<String>> headerEntry : response.getStringHeaders().entrySet()) {
        // logger.debug("header: " + headerEntry.getKey());
        // for (String val : headerEntry.getValue()) {
        // logger.debug("    " + val);
        // }
        // }
        response.close();
    }
}
