package io.quarkus.rest.test.core.encoding;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.core.encoding.resource.EncodingTestClient;
import io.quarkus.rest.test.core.encoding.resource.EncodingTestResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Encoding
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for HTML encoding and decoding.
 * @tpSince RESTEasy 3.0.16
 */
public class EncodingTest {
    static QuarkusRestClient client;

    protected static final Logger logger = Logger.getLogger(EncodingTest.class.getName());

    private static EncodingTestClient testClient;

    Character[] RESERVED_CHARACTERS = {
            '?', ':', '@', '&', '=', '+', '$', ','
    };

    //also includes a-zA-Z0-9
    Character[] UNRESERVED_CHARACTERS = {
            '-', '_', '.', '!', '~', '*', '\'', '(', ')'
    };
    //also includes 0x00-0x1F and 0x7F
    Character[] EXCLUDED_CHARACTERS = {
            ' ', '<', '>', '#', '%', '\"'
    };
    Character[] UNWISE_CHARACTERS = {
            '{', '}', '|', '\\', '^', '[', ']', '`'
    };

    @Before
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
        testClient = client.target(PortProviderUtil.generateBaseUrl(EncodingTest.class.getSimpleName()))
                .proxy(EncodingTestClient.class);
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(EncodingTestClient.class);

                    return TestUtil.finishContainerPrepare(war, null, EncodingTestResource.class);
                }
            });

    /**
     * @tpTestDetails Tests requesting special characters via a ClientProxy.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testEncodingCharacters() throws Exception {
        for (Character ch : RESERVED_CHARACTERS) {
            encodingCharacter(ch);
        }
        for (Character ch : UNRESERVED_CHARACTERS) {
            encodingCharacter(ch);
        }
        for (Character ch : EXCLUDED_CHARACTERS) {
            encodingCharacter(ch);
        }
        for (Character ch : UNWISE_CHARACTERS) {
            encodingCharacter(ch);
        }
    }

    public void encodingCharacter(Character toTest) {
        String paramWithChar = "start" + toTest + "end";
        Response returned = testClient.getPathParam(paramWithChar);
        Assert.assertNotNull("Wrong returned value", returned);
        Assert.assertEquals("Wrong returned status", returned.getStatus(), HttpURLConnection.HTTP_OK);
        Assert.assertEquals("Wrong returned value", returned.readEntity(String.class), paramWithChar);
    }

    /**
     * @tpTestDetails Tests backslash encoding.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testPercent() {
        encodingCharacter('\\');
    }

    /**
     * @tpTestDetails Tests requesting special characters via manual URL construction.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testViaDirectURI() throws Exception {
        for (Character ch : RESERVED_CHARACTERS) {
            viaDirectURI(ch);
        }
        for (Character ch : UNRESERVED_CHARACTERS) {
            viaDirectURI(ch);
        }
        for (Character ch : EXCLUDED_CHARACTERS) {
            viaDirectURI(ch);
        }
        for (Character ch : UNWISE_CHARACTERS) {
            viaDirectURI(ch);
        }

    }

    public void viaDirectURI(Character toTest) throws Exception {
        String expected = "start" + toTest + "end";
        String encoded = "start%" + Integer.toHexString(toTest).toUpperCase() + "end";
        String stringUri = PortProviderUtil.generateURL("/test/path-param/" + encoded, EncodingTest.class.getSimpleName());
        URI uri = URI.create(stringUri);
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("accept", "text/plain");
        InputStream is = connection.getInputStream();
        Reader r = new InputStreamReader(is, StandardCharsets.UTF_8);
        StringBuffer buf = new StringBuffer();
        char[] chars = new char[1024];
        int charsRead;
        while ((charsRead = r.read(chars)) != -1) {
            buf.append(chars, 0, charsRead);
        }
        r.close();
        is.close();

        Assert.assertEquals("Wrong answer (answer may be decoded badly)", buf.toString(), expected);
    }

    public void testPathParamWithDoublePercent() {
        String paramWithDoublePercent = "start%%end";
        Response returned = testClient.getPathParam(paramWithDoublePercent);
        Assert.assertNotNull(returned);
        Assert.assertEquals(HttpURLConnection.HTTP_OK, returned.getStatus());
        Assert.assertEquals(paramWithDoublePercent, returned.readEntity(String.class));
    }

    /**
     * @tpTestDetails Test path for {} characters
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testPathParamWithBraces() {
        String paramWithBraces = "start{param}end";
        Response returned = testClient.getPathParam(paramWithBraces);
        Assert.assertNotNull("Wrong content of response", returned);
        Assert.assertEquals(HttpURLConnection.HTTP_OK, returned.getStatus());
        Assert.assertEquals("Wrong content of response", paramWithBraces, returned.readEntity(String.class));
    }

    /**
     * @tpTestDetails Test path for % character
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testPathParamWithLifePercentDeath() {
        String paramWithLifePercentDeath = "life%death";
        Response returned = testClient.getPathParam(paramWithLifePercentDeath);
        Assert.assertNotNull("Wrong content of response", returned);
        Assert.assertEquals(HttpURLConnection.HTTP_OK, returned.getStatus());
        Assert.assertEquals("Wrong content of response", paramWithLifePercentDeath, returned.readEntity(String.class));
    }

    /**
     * @tpTestDetails Test query for %% characters
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testQueryParamWithDoublePercent() {
        String paramWithDoublePercent = "start%%end";
        Response returned = testClient.getQueryParam(paramWithDoublePercent);
        Assert.assertNotNull("Wrong content of response", returned);
        Assert.assertEquals(HttpURLConnection.HTTP_OK, returned.getStatus());
        Assert.assertEquals("Wrong content of response", paramWithDoublePercent, returned.readEntity(String.class));
    }

    /**
     * @tpTestDetails Test query for {} characters
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testQueryParamWithBraces() {
        String paramWithBraces = "start{param}end";
        Response returned = testClient.getQueryParam(paramWithBraces);
        Assert.assertNotNull("Wrong content of response", returned);
        Assert.assertEquals(HttpURLConnection.HTTP_OK, returned.getStatus());
        Assert.assertEquals("Wrong content of response", paramWithBraces, returned.readEntity(String.class));
    }

    /**
     * @tpTestDetails Test query for % character
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testQueryParamWithLifePercentDeath() {
        String paramWithLifePercentDeath = "life%death";
        Response returned = testClient.getQueryParam(paramWithLifePercentDeath);
        Assert.assertNotNull("Wrong content of response", returned);
        Assert.assertEquals(HttpURLConnection.HTTP_OK, returned.getStatus());
        Assert.assertEquals("Wrong content of response", paramWithLifePercentDeath, returned.readEntity(String.class));
    }
}
