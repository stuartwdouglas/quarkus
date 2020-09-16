package io.quarkus.rest.test.response;

import static junit.framework.TestCase.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.response.resource.VariantAcceptResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-994
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Variant Accept Test")
public class VariantAcceptTest {

    public static final MediaType WILDCARD_WITH_PARAMS;

    public static final MediaType TEXT_HTML_WITH_PARAMS;

    public static final MediaType TEXT_PLAIN_WITH_PARAMS;

    static {
        Map<String, String> params = new HashMap<String, String>();
        params.put("q", "0.5");
        params.put("a", "1");
        params.put("b", "2");
        params.put("c", "3");
        WILDCARD_WITH_PARAMS = new MediaType("*", "*", params);
        params.clear();
        params.put("a", "1");
        params.put("b", "2");
        params.put("c", "3");
        TEXT_HTML_WITH_PARAMS = new MediaType("text", "html", params);
        params.clear();
        params.put("a", "1");
        params.put("b", "2");
        params.put("c", "3");
        TEXT_PLAIN_WITH_PARAMS = new MediaType("text", "plain", params);
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(VariantAcceptTest.class);
            return TestUtil.finishContainerPrepare(war, null, VariantAcceptResource.class);
        }
    });

    protected Client client;

    @BeforeEach
    public void beforeTest() {
        client = ClientBuilder.newClient();
    }

    @AfterEach
    public void afterTest() {
        client.close();
        client = null;
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, VariantAcceptTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Verifies that a more specific media type is preferred.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Variant")
    public void testVariant() throws Exception {
        Invocation.Builder request = client.target(generateURL("/variant")).request();
        request.accept(MediaType.WILDCARD_TYPE);
        request.accept(MediaType.TEXT_HTML_TYPE);
        Response response = request.get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        assertEquals(MediaType.TEXT_HTML, entity, "Wrong media type on response");
    }

    /**
     * @tpTestDetails Verifies that the number of parameters does not outweigh more specific media types.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Variant With Parameters")
    public void testVariantWithParameters() throws Exception {
        Invocation.Builder request = client.target(generateURL("/params")).request();
        request.accept(WILDCARD_WITH_PARAMS);
        request.accept(MediaType.TEXT_HTML_TYPE);
        Response response = request.get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        assertEquals(TEXT_HTML_WITH_PARAMS.toString(), entity, "Wrong media type on response");
    }

    /**
     * @tpTestDetails Verifies that the q/qs factors are stripped from the response Content-type header if they are provided
     *                in the request/@Produces. See RESTEASY-1765.
     * @tpSince RESTEasy 3.0.25
     */
    @Test
    @DisplayName("Test Variant With Q Parameter")
    public void testVariantWithQParameter() throws Exception {
        Invocation.Builder request = client.target(generateURL("/simple")).request();
        request.accept("application/json;q=0.3, application/xml;q=0.2");
        Response response = request.get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals(response.getHeaderString("Content-Type"), "application/json");
        request = client.target(generateURL("/simpleqs")).request();
        response = request.get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals(response.getHeaderString("Content-Type"), "application/xml;charset=UTF-8");
    }
}
