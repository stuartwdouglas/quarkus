package io.quarkus.rest.test.response;

import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
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
public class CookieTest {

    protected final Logger logger = LogManager.getLogger(VariantsTest.class.getName());

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
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

    @BeforeClass
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterClass
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
    public void testWeirdCookie() {
        Response response = client.target(generateURL("/cookie/weird")).request().get();
        Assert.assertEquals(response.getStatus(), 200);
        Map<String, NewCookie> cookies = response.getCookies();
        //        for (Map.Entry<String, NewCookie> cookieEntry : cookies.entrySet()) {
        //            logger.debug("[" + cookieEntry.getKey() + "] >>>>>> " + cookieEntry.getValue() + "");
        //        }
        //        for (Map.Entry<String, List<String>> headerEntry : response.getStringHeaders().entrySet()) {
        //            logger.debug("header: " + headerEntry.getKey());
        //            for (String val : headerEntry.getValue()) {
        //                logger.debug("    " + val);
        //            }
        //        }
        Assert.assertTrue("Cookie in the response doesn't contain 'guid', which was in the cookie send by the server.",
                cookies.containsKey("guid"));
        response.close();
    }

    /**
     * @tpTestDetails Client sends GET request to the server, server text response, the response is then checked, that
     *                it contains standard cookie sent by server.
     * @tpPassCrit Response contains cookie sent by server
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testStandardCookie() {
        Response response = client.target(generateURL("/cookie/standard")).request().get();
        Assert.assertEquals(response.getStatus(), 200);
        for (Map.Entry<String, NewCookie> cookieEntry : response.getCookies().entrySet()) {
            //            logger.debug("[" + cookieEntry.getKey() + "] >>>>>> " + cookieEntry.getValue() + "");
            Assert.assertEquals("Cookie content in the response doesn't match the cookie content send by the server.",
                    "UserID=JohnDoe;Version=1;Max-Age=3600", cookieEntry.getValue().toString());
        }
        //        for (Map.Entry<String, List<String>> headerEntry : response.getStringHeaders().entrySet()) {
        //            logger.debug("header: " + headerEntry.getKey());
        //            for (String val : headerEntry.getValue()) {
        //                logger.debug("    " + val);
        //            }
        //        }
        response.close();
    }

}
