package io.quarkus.rest.test.providers.jaxb;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.providers.jaxb.resource.CharSetFavoriteMovieXmlRootElement;
import io.quarkus.rest.test.providers.jaxb.resource.CharSetMovieResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1066.
 * @tpSince RESTEasy 3.0.16
 */
public class CharSetRE1066Test {
    public static final MediaType APPLICATION_XML_UTF16_TYPE;
    public static final MediaType TEXT_PLAIN_UTF16_TYPE;
    public static final MediaType WILDCARD_UTF16_TYPE;
    public static final String APPLICATION_XML_UTF16 = "application/xml;charset=UTF-16";
    public static final String TEXT_PLAIN_UTF16 = "text/plain;charset=UTF-16";
    public static final String WILDCARD_UTF16 = "*/*;charset=UTF-16";

    private final Logger log = Logger.getLogger(CharSetRE1066Test.class.getName());
    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(CharSetRE1066Test.class);
                    return TestUtil.finishContainerPrepare(war, null, CharSetMovieResource.class,
                            CharSetFavoriteMovieXmlRootElement.class);
                }
            });

    @Before
    public void before() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @After
    public void after() {
        client.close();
        client = null;
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, CharSetRE1066Test.class.getSimpleName());
    }

    static {
        Map<String, String> params = new HashMap<String, String>();
        params.put("charset", "UTF-16");
        APPLICATION_XML_UTF16_TYPE = new MediaType("application", "xml", params);
        TEXT_PLAIN_UTF16_TYPE = new MediaType("text", "plain", params);
        WILDCARD_UTF16_TYPE = new MediaType("*", "*", params);
    }

    @Test
    public void testXmlDefault() throws Exception {
        QuarkusRestWebTarget target = client.target(generateURL("/xml/default"));
        Builder request = target.request();
        request.accept(MediaType.APPLICATION_XML_TYPE);

        String str = "<?xml version=\"1.0\"?>\r"
                + "<charSetFavoriteMovieXmlRootElement><title>La Règle du Jeu</title></charSetFavoriteMovieXmlRootElement>";
        log.info(str);
        log.info("client default charset: " + Charset.defaultCharset());
        log.info("Sending request");

        Response response = request.post(Entity.entity(str, MediaType.APPLICATION_XML_TYPE));
        log.info("Received response");

        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        CharSetFavoriteMovieXmlRootElement entity = response.readEntity(CharSetFavoriteMovieXmlRootElement.class);
        log.info("Result: " + entity);
        log.info("title: " + entity.getTitle());
        Assert.assertEquals("La Règle du Jeu", entity.getTitle());
    }

    @Test
    public void testXmlProduces() throws Exception {
        QuarkusRestWebTarget target = client.target(generateURL("/xml/produces"));
        Builder request = target.request();

        String str = "<?xml version=\"1.0\"?>\r"
                + "<charSetFavoriteMovieXmlRootElement><title>La Règle du Jeu</title></charSetFavoriteMovieXmlRootElement>";
        log.info(str);
        log.info("client default charset: " + Charset.defaultCharset());

        Response response = request.post(Entity.entity(str, APPLICATION_XML_UTF16_TYPE));
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        CharSetFavoriteMovieXmlRootElement entity = response.readEntity(CharSetFavoriteMovieXmlRootElement.class);
        log.info("Result: " + entity);
        log.info("title: " + entity.getTitle());
        Assert.assertEquals("La Règle du Jeu", entity.getTitle());
    }

    @Test
    public void testXmlAccepts() throws Exception {
        QuarkusRestWebTarget target = client.target(generateURL("/xml/accepts"));
        Builder request = target.request();
        request.accept(APPLICATION_XML_UTF16_TYPE);

        String str = "<?xml version=\"1.0\"?>\r"
                + "<charSetFavoriteMovieXmlRootElement><title>La Règle du Jeu</title></charSetFavoriteMovieXmlRootElement>";
        log.info(str);
        log.info("client default charset: " + Charset.defaultCharset());

        Response response = request.post(Entity.entity(str, APPLICATION_XML_UTF16_TYPE));
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        CharSetFavoriteMovieXmlRootElement entity = response.readEntity(CharSetFavoriteMovieXmlRootElement.class);
        log.info("Result: " + entity);
        log.info("title: " + entity.getTitle());
        Assert.assertEquals("La Règle du Jeu", entity.getTitle());
    }
}
