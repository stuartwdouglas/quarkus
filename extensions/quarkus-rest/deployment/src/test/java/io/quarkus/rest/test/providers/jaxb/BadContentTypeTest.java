package io.quarkus.rest.test.providers.jaxb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.jaxb.resource.BadContenTypeTestResource;
import io.quarkus.rest.test.providers.jaxb.resource.BadContentTypeTestBean;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class BadContentTypeTest {

    private static Logger logger = Logger.getLogger(BadContentTypeTest.class.getName());
    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, BadContenTypeTestResource.class,
                            BadContentTypeTestBean.class);
                }
            });

    @Before
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, BadContentTypeTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Tests if correct Response code is returned when sending syntactically incorrect xml
     * @tpInfo RESTEASY-519
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testBadRequest() throws Exception {
        ResteasyWebTarget target = client.target(generateURL("/test"));
        Response response = target.request().post(Entity.entity("<junk", "application/xml"));
        Assert.assertEquals("The returned response status is not the expected one",
                Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    /**
     * @tpTestDetails Tests if correct exception and MessageBodyWriter error is thrown when sending request for which no
     *                MessageBodyWriterExists
     * @tpInfo RESTEASY-169
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testHtmlError() throws Exception {
        ResteasyWebTarget target = client.target(generateURL("/test"));
        Response response = target.request().header("Accept", "text/html").get();
        String stringResp = response.readEntity(String.class);
        logger.info("response: " + stringResp);
        assertEquals("The returned response status is not the expected one",
                Status.INTERNAL_SERVER_ERROR, response.getStatus());
        assertTrue("The unexpected error response was thrown", stringResp.contains("media type: text/html"));
    }

    /**
     * @tpTestDetails Tests if correct HTTP 406 status code is returned when the specified accept media type
     *                is not supported by the server
     */
    @Test
    public void testNotAcceptable() throws Exception {
        ResteasyWebTarget target = client.target(generateURL("/test/foo"));
        Response response = target.request().header("Accept", "text/plain").get();
        assertEquals("The returned response status is not the expected one",
                Status.NOT_ACCEPTABLE, response.getStatus());
    }

    /**
     * @tpTestDetails Tests of receiving Bad Request response code after html error
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testBadRequestAfterHtmlError() throws Exception {
        ResteasyWebTarget target = client.target(generateURL("/test"));
        Response response = target.request().post(Entity.entity("<junk", "application/xml"));
        Assert.assertEquals("The returned response status is not the expected one",
                Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        response.close();

        response = target.request().header("Accept", "text/html").get();
        String stringResp = response.readEntity(String.class);
        logger.info("response: " + stringResp);
        assertEquals("The returned response status is not the expected one",
                Status.INTERNAL_SERVER_ERROR, response.getStatus());
        assertTrue("The unexpected error response was thrown", stringResp.contains("media type: text/html"));

    }

}
