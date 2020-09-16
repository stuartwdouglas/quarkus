package io.quarkus.rest.test.response;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.response.resource.ContentLanguageHeaderResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy Response
 * @tpChapter Integration tests
 * @tpTestCaseDetails Check presence of Content-Language header in a response
 * @tpSince RESTEasy 3.8.0
 */
@DisplayName("Content Language Header Test")
public class ContentLanguageHeaderTest {

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, ContentLanguageHeaderResource.class);
        }
    });

    @BeforeAll
    public static void init() {
        client = ClientBuilder.newBuilder().build();
    }

    @AfterAll
    public static void after() {
        client.close();
    }

    /**
     * @tpTestDetails Test for Content-Language header set by ResponseBuilder.language method.
     * @tpSince RESTEasy 3.8.0
     */
    @Test
    @DisplayName("Test Language")
    public void testLanguage() {
        Response response = client
                .target(PortProviderUtil.generateURL("/language", ContentLanguageHeaderTest.class.getSimpleName())).request()
                .get();
        MultivaluedMap<String, Object> headers = response.getHeaders();
        Assertions.assertTrue(headers.keySet().contains("Content-Language"),
                "Content-Language header is not present in response");
        Assertions.assertEquals("en-us", headers.getFirst("Content-Language"),
                "Content-Language header does not have expected value");
    }

    /**
     * @tpTestDetails Test for Content-Language header set as Variant by Response.ok method.
     * @tpSince RESTEasy 3.8.0
     */
    @Test
    @DisplayName("Test Language Ok")
    public void testLanguageOk() {
        Response response = client
                .target(PortProviderUtil.generateURL("/language-ok", ContentLanguageHeaderTest.class.getSimpleName())).request()
                .get();
        MultivaluedMap<String, Object> headers = response.getHeaders();
        Assertions.assertTrue(headers.keySet().contains("Content-Language"),
                "Content-Language header is not present in response");
        Assertions.assertEquals("en-us", headers.getFirst("Content-Language"),
                "Content-Language header does not have expected value");
    }

    /**
     * @tpTestDetails Test for Content-Language header set as Variant by ResponseBuilder.variant method.
     * @tpSince RESTEasy 3.8.0
     */
    @Test
    @DisplayName("Test Language Variant")
    public void testLanguageVariant() {
        Response response = client
                .target(PortProviderUtil.generateURL("/language-variant", ContentLanguageHeaderTest.class.getSimpleName()))
                .request().get();
        MultivaluedMap<String, Object> headers = response.getHeaders();
        Assertions.assertTrue(headers.keySet().contains("Content-Language"),
                "Content-Language header is not present in response");
        Assertions.assertEquals("en-us", headers.getFirst("Content-Language"),
                "Content-Language header does not have expected value");
    }
}
