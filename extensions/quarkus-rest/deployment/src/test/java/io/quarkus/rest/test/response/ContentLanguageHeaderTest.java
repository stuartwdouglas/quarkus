package io.quarkus.rest.test.response;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MultivaluedMap;
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
public class ContentLanguageHeaderTest {

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, ContentLanguageHeaderResource.class);
                }
            });

    @BeforeClass
    public static void init() {
        client = ClientBuilder.newBuilder().build();
    }

    @AfterClass
    public static void after() {
        client.close();
    }

    /**
     * @tpTestDetails Test for Content-Language header set by ResponseBuilder.language method.
     * @tpSince RESTEasy 3.8.0
     */
    @Test
    public void testLanguage() {
        Response response = client
                .target(PortProviderUtil.generateURL("/language", ContentLanguageHeaderTest.class.getSimpleName())).request()
                .get();
        MultivaluedMap<String, Object> headers = response.getHeaders();

        Assert.assertTrue("Content-Language header is not present in response", headers.keySet().contains("Content-Language"));
        Assert.assertEquals("Content-Language header does not have expected value", "en-us",
                headers.getFirst("Content-Language"));
    }

    /**
     * @tpTestDetails Test for Content-Language header set as Variant by Response.ok method.
     * @tpSince RESTEasy 3.8.0
     */
    @Test
    public void testLanguageOk() {
        Response response = client
                .target(PortProviderUtil.generateURL("/language-ok", ContentLanguageHeaderTest.class.getSimpleName())).request()
                .get();
        MultivaluedMap<String, Object> headers = response.getHeaders();

        Assert.assertTrue("Content-Language header is not present in response", headers.keySet().contains("Content-Language"));
        Assert.assertEquals("Content-Language header does not have expected value", "en-us",
                headers.getFirst("Content-Language"));
    }

    /**
     * @tpTestDetails Test for Content-Language header set as Variant by ResponseBuilder.variant method.
     * @tpSince RESTEasy 3.8.0
     */
    @Test
    public void testLanguageVariant() {
        Response response = client
                .target(PortProviderUtil.generateURL("/language-variant", ContentLanguageHeaderTest.class.getSimpleName()))
                .request().get();
        MultivaluedMap<String, Object> headers = response.getHeaders();

        Assert.assertTrue("Content-Language header is not present in response", headers.keySet().contains("Content-Language"));
        Assert.assertEquals("Content-Language header does not have expected value", "en-us",
                headers.getFirst("Content-Language"));
    }

}
