package io.quarkus.rest.test.providers.plain;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.jaxb.CharSetTest;
import io.quarkus.rest.test.providers.plain.resource.CharsetFoo;
import io.quarkus.rest.test.providers.plain.resource.CharsetResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Plain provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1066. If the content-type of the response is not specified in the request,
 *
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Charset Test")
public class CharsetTest {

    protected static final Logger logger = Logger.getLogger(CharSetTest.class.getName());

    static QuarkusRestClient client;

    protected static final MediaType TEXT_PLAIN_UTF16_TYPE;

    protected static final MediaType WILDCARD_UTF16_TYPE;

    private static Map<String, String> params;

    static {
        params = new HashMap<String, String>();
        params.put("charset", "UTF-16");
        TEXT_PLAIN_UTF16_TYPE = new MediaType("text", "plain", params);
        WILDCARD_UTF16_TYPE = new MediaType("*", "*", params);
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, params, CharsetResource.class, CharsetFoo.class);
        }
    });

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, CharSetTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Tests StringTextStar provider, where the charset is unspecified.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test String Default")
    public void testStringDefault() throws Exception {
        logger.info("client default charset: " + Charset.defaultCharset());
        WebTarget target = client.target(generateURL("/accepts/string/default"));
        String str = "La Règle du Jeu";
        logger.info(str);
        Response response = target.request().post(Entity.entity(str, MediaType.WILDCARD_TYPE));
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        logger.info("Result: " + entity);
        Assertions.assertEquals(str, entity, "The response from the server is not what was expected");
    }

    /**
     * @tpTestDetails Tests StringTextStar provider, where the charset is specified
     *                by the resource method.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test String Produces Utf 16")
    public void testStringProducesUtf16() throws Exception {
        WebTarget target = client.target(generateURL("/produces/string/utf16"));
        String str = "La Règle du Jeu";
        logger.info(str);
        Response response = target.request().post(Entity.entity(str, TEXT_PLAIN_UTF16_TYPE));
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        logger.info("Result: " + entity);
        Assertions.assertEquals(str, entity, "The response from the server is not what was expected");
    }

    /**
     * @tpTestDetails Tests StringTextStar provider, where the charset is specified
     *                by the Accept header.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test String Accepts Utf 16")
    public void testStringAcceptsUtf16() throws Exception {
        WebTarget target = client.target(generateURL("/accepts/string/default"));
        String str = "La Règle du Jeu";
        logger.info(str);
        Response response = target.request().accept(WILDCARD_UTF16_TYPE).post(Entity.entity(str, TEXT_PLAIN_UTF16_TYPE));
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        logger.info("Result: " + entity);
        Assertions.assertEquals(str, entity, "The response from the server is not what was expected");
    }

    /**
     * @tpTestDetails Tests DefaultTextPlain provider, where the charset is unspecified.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Foo Default")
    public void testFooDefault() throws Exception {
        WebTarget target = client.target(generateURL("/accepts/foo/default"));
        CharsetFoo foo = new CharsetFoo("La Règle du Jeu");
        logger.info(foo);
        Response response = target.request().post(Entity.entity(foo, MediaType.TEXT_PLAIN_TYPE));
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        logger.info("Result: " + entity);
        Assertions.assertEquals(foo.valueOf(), entity, "The response from the server is not what was expected");
    }

    /**
     * @tpTestDetails Tests DefaultTextPlain provider, where the charset is specified
     *                by the resource method.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Foo Produces Utf 16")
    public void testFooProducesUtf16() throws Exception {
        WebTarget target = client.target(generateURL("/produces/foo/utf16"));
        CharsetFoo foo = new CharsetFoo("La Règle du Jeu");
        logger.info(foo);
        Response response = target.request().post(Entity.entity(foo, TEXT_PLAIN_UTF16_TYPE));
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        logger.info("Result: " + entity);
        Assertions.assertEquals(foo.valueOf(), entity, "The response from the server is not what was expected");
    }

    /**
     * @tpTestDetails Tests DefaultTextPlain provider, where the charset is specified
     *                by the Accept header.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Foo Accepts Utf 16")
    public void testFooAcceptsUtf16() throws Exception {
        WebTarget target = client.target(generateURL("/accepts/foo/default"));
        CharsetFoo foo = new CharsetFoo("La Règle du Jeu");
        logger.info(foo);
        Response response = target.request().accept(TEXT_PLAIN_UTF16_TYPE).post(Entity.entity(foo, TEXT_PLAIN_UTF16_TYPE));
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        logger.info("Result: " + entity);
        Assertions.assertEquals(foo.valueOf(), entity, "The response from the server is not what was expected");
    }

    /**
     * @tpTestDetails Tests StringTextStar provider, default charset
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Form Default")
    public void testFormDefault() throws Exception {
        WebTarget target = client.target(generateURL("/accepts/form/default"));
        Form form = new Form().param("title", "La Règle du Jeu");
        Response response = target.request().post(Entity.form(form));
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("title=La Règle du Jeu", response.readEntity(String.class),
                "The response from the server is not what was expected");
    }
}
