package io.quarkus.rest.test.response;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.response.resource.HttponlyCookieResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter NewCookie httponly flag is processed
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.1.0.Final
 */
public class HttponlyCookieTest {

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, HttponlyCookieResource.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, HttponlyCookieTest.class.getSimpleName());
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

    @Test
    public void testHttponlyTrue() {
        WebTarget target = client.target(generateURL("/cookie/true"));
        Response response = target.request().get();
        NewCookie cookie = response.getCookies().entrySet().iterator().next().getValue();
        Assert.assertNotNull(cookie);
        Assert.assertTrue(cookie.isHttpOnly());
    }

    @Test
    public void testHttponlyDefault() {
        WebTarget target = client.target(generateURL("/cookie/default"));
        Response response = target.request().get();
        NewCookie cookie = response.getCookies().entrySet().iterator().next().getValue();
        Assert.assertNotNull(cookie);
        Assert.assertFalse(cookie.isHttpOnly());
    }
}
