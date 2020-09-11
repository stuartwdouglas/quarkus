package io.quarkus.rest.test.core.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.core.basic.resource.ContextAfterEncoderInterceptor;
import io.quarkus.rest.test.core.basic.resource.ContextBeforeEncoderInterceptor;
import io.quarkus.rest.test.core.basic.resource.ContextEncoderInterceptor;
import io.quarkus.rest.test.core.basic.resource.ContextEndInterceptor;
import io.quarkus.rest.test.core.basic.resource.ContextService;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpSince RESTEasy 3.0.16
 * @tpTestCaseDetails Regression for RESTEASY-699
 */
public class ContextTest {
    public static final String WRONG_RESPONSE_ERROR_MSG = "Wrong content of response";

    private static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(ContextAfterEncoderInterceptor.class, ContextBeforeEncoderInterceptor.class,
                            ContextService.class,
                            ContextEncoderInterceptor.class, ContextEndInterceptor.class);
                    war.addAsWebInfResource(ContextTest.class.getPackage(), "ContextIndex.html", "index.html");
                    war.addAsWebInfResource(ContextTest.class.getPackage(), "ContextWeb.xml", "web.xml");
                    // undertow requires read permission in order to perform forward request.

                    return war;
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ContextTest.class.getSimpleName());
    }

    @Before
    public void setup() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Test for forwarding request to external HTML file
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testForward() throws Exception {
        Response response = client.target(generateURL("/test/forward")).request().get();
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("Wrong content of response", "hello world", response.readEntity(String.class));
        response.close();
    }

    /**
     * @tpTestDetails Base URL should not be affected by URL parameter
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testRepeat() throws Exception {
        Response response = client.target(generateURL("/test/test")).request().get();
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("Resource get wrong injected URL", generateURL("/test/"), response.readEntity(String.class));
        response.close();
        response = client.target(generateURL("/test/")).request().get();
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("Resource get wrong injected URL", generateURL("/test/"), response.readEntity(String.class));
        response.close();
    }

    /**
     * @tpTestDetails Test for getting servlet context in REST resource
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testServletContext() throws Exception {
        final String HEADER_ERROR_MESSAGE = "Response don't have correct headers";
        Response response = client.target(generateURL("/test/test/servletcontext")).request().get();
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals(WRONG_RESPONSE_ERROR_MSG, "ok", response.readEntity(String.class));
        Assert.assertNotNull(HEADER_ERROR_MESSAGE, response.getHeaderString("before-encoder"));
        Assert.assertNotNull(HEADER_ERROR_MESSAGE, response.getHeaderString("after-encoder"));
        Assert.assertNotNull(HEADER_ERROR_MESSAGE, response.getHeaderString("end"));
        Assert.assertNotNull(HEADER_ERROR_MESSAGE, response.getHeaderString("encoder"));
        response.close();
    }

    /**
     * @tpTestDetails Test for getting servlet config in REST resource
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testServletConfig() throws Exception {
        Response response = client.target(generateURL("/test/test/servletconfig")).request().get();
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals(WRONG_RESPONSE_ERROR_MSG, "ok", response.readEntity(String.class));
        response.close();
    }

    /**
     * @tpTestDetails XML extension mapping test
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testXmlMappings() throws Exception {
        Response response = client.target(generateURL("/test/stuff.xml")).request().get();
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals(WRONG_RESPONSE_ERROR_MSG, "xml", response.readEntity(String.class));
        response.close();

    }

    /**
     * @tpTestDetails Json extension mapping test
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testJsonMappings() throws Exception {
        Response response = client.target(generateURL("/test/stuff.json")).request().get();
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals(WRONG_RESPONSE_ERROR_MSG, "json", response.readEntity(String.class));
        response.close();
    }
}
