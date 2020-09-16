package io.quarkus.rest.test.response;

import java.util.List;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.delegates.LocaleDelegate;
import org.jboss.resteasy.util.HttpHeaderNames;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.response.resource.VariantComplexResource;
import io.quarkus.rest.test.response.resource.VariantEncodingResource;
import io.quarkus.rest.test.response.resource.VariantLanguageResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Response
 * @tpChapter Integration tests
 * @tpTestCaseDetails Tests that correct variant headers are returned in the response
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Variants Test")
public class VariantsTest {

    protected static final Logger logger = Logger.getLogger(VariantsTest.class.getName());

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(VariantsTest.class);
            return TestUtil.finishContainerPrepare(war, null, VariantLanguageResource.class, VariantComplexResource.class,
                    VariantEncodingResource.class);
        }
    });

    @BeforeEach
    public void init() {
        client = ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, VariantsTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Simple http GET conditional request
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Evaluate Preconditions Tag Null And Simple Get Test")
    public void evaluatePreconditionsTagNullAndSimpleGetTest() {
        logger.info(generateURL("/preconditionsSimpleGet"));
        Response response = client.target(generateURL("/preconditionsSimpleGet")).request().get();
        Assertions.assertEquals(200, response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails PUT request is send, request selects best variant from the list od empty variants,
     *                IllegalArgumentException is thrown
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Select Variant Put Request Test")
    public void selectVariantPutRequestTest() {
        Response response = client.target(generateURL("/SelectVariantTestPut")).request().put(null);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "PASSED");
        response.close();
    }

    /**
     * @tpTestDetails GET request is send, test that variant is selected and response contains proper headers
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Select Variant Response Vary Test")
    public void selectVariantResponseVaryTest() {
        Response response = client.target(generateURL("/SelectVariantTestResponse")).request().accept("application/json")
                .acceptEncoding("*").get();
        Assertions.assertEquals(200, response.getStatus());
        List<String> headers = response.getStringHeaders().get("Vary");
        Assertions.assertEquals(1, headers.size());
        String vary = headers.get(0);
        logger.info(vary);
        Assertions.assertTrue(vary.contains("Accept-Language"));
        Assertions.assertTrue(vary.contains("Accept-Encoding"));
        Assertions.assertTrue(vary.matches(".*Accept.*Accept.*Accept.*"));
        response.close();
    }

    /**
     * @tpTestDetails Tests that variant preferred by client request by Accept-Language is selected
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Language En")
    public void testGetLanguageEn() throws Exception {
        Response response = client.target(generateURL("/")).request().acceptLanguage("en").get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "en");
        Assertions.assertEquals(response.getLanguage().toString(), "en");
        response.close();
    }

    /**
     * @tpTestDetails Tests that given wildcard client request by Accept-Language header returns some concrete language
     *                header in the response.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Language Wildcard")
    public void testGetLanguageWildcard() throws Exception {
        Response response = client.target(generateURL("/")).request().acceptLanguage("*").get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertNotNull(response.getLanguage());
        response.close();
    }

    /**
     * @tpTestDetails Tests that variant preferred by client request by Accept-Language is selected. Variant defined with
     *                Locale("pt", "BR")
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Language Sub Local")
    public void testGetLanguageSubLocal() throws Exception {
        Response response = client.target(generateURL("/brazil")).request().acceptLanguage("pt").get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertNotNull(response.getLanguage());
        response.close();
    }

    /**
     * @tpTestDetails Test that language variant which has 0 preference is not returned
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Language Zero")
    public void testGetLanguageZero() throws Exception {
        Response response = client.target(generateURL("/")).request().acceptLanguage("*", "zh;q=0", "en;q=0", "fr;q=0").get();
        Assertions.assertEquals(Status.NOT_ACCEPTABLE.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Simple Get for with Accept-Language "zh". Lanfuage "zh" in the response is returned
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Language Zh")
    public void testGetLanguageZh() throws Exception {
        Response response = client.target(generateURL("/")).request().acceptLanguage("zh").get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "zh");
        Assertions.assertEquals(response.getLanguage().toString(), "zh");
        response.close();
    }

    /**
     * @tpTestDetails Tests client request with multiple Accept-language preferences, check the most preferred language
     *                is returned in the response
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Language Multiple")
    public void testGetLanguageMultiple() throws Exception {
        Response response = client.target(generateURL("/")).request().acceptLanguage("en;q=0.3", "zh;q=0.4", "fr").get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "fr");
        Assertions.assertEquals(response.getLanguage().toString(), "fr");
        response.close();
    }

    /**
     * @tpTestDetails Verifies that a more specific media type is preferred.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Complex Accept Language En")
    public void testGetComplexAcceptLanguageEn() throws Exception {
        Response response = client
                .target(generateURL("/complex")).request().accept("text/xml", "application/xml", "application/xhtml+xml",
                        "image/png", "text/html;q=0.9", "text/plain;q=0.8", "*/*;q=0.5")
                .acceptLanguage("en-us", "en;q=0.5").get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "GET");
        Assertions.assertEquals(MediaType.APPLICATION_XML_TYPE.withCharset("UTF-8"), response.getMediaType());
        Assertions.assertEquals(new LocaleDelegate().toString(response.getLanguage()), "en-us");
        response.close();
    }

    /**
     * @tpTestDetails Verifies that a more specific media type is preferred.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Complex Accept Language En Us")
    public void testGetComplexAcceptLanguageEnUs() throws Exception {
        Response response = client.target(generateURL("/complex")).request().accept("text/xml", "application/xml",
                "application/xhtml+xml", "image/png", "text/html;q=0.9", "text/plain;q=0.8", "*/*;q=0.5")
                .acceptLanguage("en", "en-us").get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "GET");
        Assertions.assertEquals(MediaType.APPLICATION_XML_TYPE.withCharset("UTF-8"), response.getMediaType());
        Assertions.assertEquals(new LocaleDelegate().toString(response.getLanguage()), "en-us");
        response.close();
    }

    /**
     * @tpTestDetails Test that expected variants are selected from list of multiple weighted content and language type.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Complex Shuffle Accept Media")
    public void testGetComplexShuffleAcceptMedia() throws Exception {
        Response response = client
                .target(generateURL("/complex")).request().accept("application/xml", "text/xml", "application/xhtml+xml",
                        "image/png", "text/html;q=0.9", "text/plain;q=0.8", "*/*;q=0.5")
                .acceptLanguage("en-us", "en;q=0.5").get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "GET");
        Assertions.assertEquals(MediaType.APPLICATION_XML_TYPE.withCharset("UTF-8"), response.getMediaType());
        Assertions.assertEquals(new LocaleDelegate().toString(response.getLanguage()), "en-us");
        response.close();
    }

    /**
     * @tpTestDetails Test that expected variants are selected from list of multiple weighted content and language type.
     *                en-us with preference
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Complex Accept Language En Us With Preference")
    public void testGetComplexAcceptLanguageEnUsWithPreference() throws Exception {
        Response response = client
                .target(generateURL("/complex")).request().accept("application/xml", "text/xml", "application/xhtml+xml",
                        "image/png", "text/html;q=0.9", "text/plain;q=0.8", "*/*;q=0.5")
                .acceptLanguage("en", "en-us;q=0.5").get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "GET");
        Assertions.assertEquals(response.getLanguage().toString(), "en");
        Assertions.assertEquals(MediaType.TEXT_XML_TYPE.withCharset("UTF-8"), response.getMediaType());
        response.close();
    }

    /**
     * @tpTestDetails Tests client request which has accept header which cannot be served by server.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Complex Not Acceptable")
    public void testGetComplexNotAcceptable() throws Exception {
        {
            Response response = client.target(generateURL("/complex")).request().accept("application/atom+xml")
                    .acceptLanguage("en-us", "en").get();
            Assertions.assertEquals(406, response.getStatus());
            String vary = response.getHeaderString(HttpHeaderNames.VARY);
            Assertions.assertNotNull(vary);
            logger.info("vary: " + vary);
            Assertions.assertTrue(contains(vary, "Accept"));
            Assertions.assertTrue(contains(vary, "Accept-Language"));
            response.close();
        }
        {
            Response response = client.target(generateURL("/complex")).request().accept("application/xml").acceptLanguage("fr")
                    .get();
            Assertions.assertEquals(406, response.getStatus());
            String vary = response.getHeaderString(HttpHeaderNames.VARY);
            Assertions.assertNotNull(vary);
            logger.info("vary: " + vary);
            Assertions.assertTrue(contains(vary, "Accept"));
            Assertions.assertTrue(contains(vary, "Accept-Language"));
            response.close();
        }
    }

    /**
     * @tpTestDetails Tests client request with custom Accept-encoding
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Encoding Custom Enc 1")
    public void testGetEncodingCustomEnc1() throws Exception {
        Response response = client.target(generateURL("/encoding")).request().acceptEncoding("enc1").get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "enc1");
        Assertions.assertEquals(response.getHeaderString(HttpHeaderNames.CONTENT_ENCODING), "enc1");
        response.close();
    }

    /**
     * @tpTestDetails Tests client request with custom Accept-encoding
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Encoding Custom Enc 2")
    public void testGetEncodingCustomEnc2() throws Exception {
        Response response = client.target(generateURL("/encoding")).request().acceptEncoding("enc2").get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "enc2");
        Assertions.assertEquals(response.getHeaderString(HttpHeaderNames.CONTENT_ENCODING), "enc2");
        response.close();
    }

    /**
     * @tpTestDetails Tests client request with custom Accept-encoding
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Encoding Custom Enc 3")
    public void testGetEncodingCustomEnc3() throws Exception {
        Response response = client.target(generateURL("/encoding")).request().acceptEncoding("enc3").get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "enc3");
        Assertions.assertEquals(response.getHeaderString(HttpHeaderNames.CONTENT_ENCODING), "enc3");
        response.close();
    }

    /**
     * @tpTestDetails Tests client request with custom Accept-encoding with preference specified
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Encoding Custom Preference")
    public void testGetEncodingCustomPreference() throws Exception {
        Response response = client.target(generateURL("/encoding")).request().acceptEncoding("enc1;q=0.5", "enc2;q=0.9").get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "enc2");
        Assertions.assertEquals(response.getHeaderString(HttpHeaderNames.CONTENT_ENCODING), "enc2");
        response.close();
    }

    /**
     * @tpTestDetails Tests client request with custom Accept-encoding with preference specified
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Encoding Custom Preference Zero")
    public void testGetEncodingCustomPreferenceZero() throws Exception {
        Response response = client.target(generateURL("/encoding")).request()
                .acceptEncoding("enc1;q=0", "enc2;q=0.888", "enc3;q=0.889").get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "enc3");
        Assertions.assertEquals(response.getHeaderString(HttpHeaderNames.CONTENT_ENCODING), "enc3");
        response.close();
    }

    private boolean contains(String all, String one) {
        String[] allSplit = all.split(",");
        for (String s : allSplit) {
            s = s.trim();
            if (s.equalsIgnoreCase(one)) {
                return true;
            }
        }
        return false;
    }
}
