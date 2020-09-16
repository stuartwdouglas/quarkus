package io.quarkus.rest.test.providers.jaxb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
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
import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
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
@DisplayName("Bad Content Type Test")
public class BadContentTypeTest {

    private static Logger logger = Logger.getLogger(BadContentTypeTest.class.getName());

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, BadContenTypeTestResource.class, BadContentTypeTestBean.class);
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
        return PortProviderUtil.generateURL(path, BadContentTypeTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Tests if correct Response code is returned when sending syntactically incorrect xml
     * @tpInfo RESTEASY-519
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Bad Request")
    public void testBadRequest() throws Exception {
        QuarkusRestWebTarget target = client.target(generateURL("/test"));
        Response response = target.request().post(Entity.entity("<junk", "application/xml"));
        Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(),
                "The returned response status is not the expected one");
    }

    /**
     * @tpTestDetails Tests if correct exception and MessageBodyWriter error is thrown when sending request for which no
     *                MessageBodyWriterExists
     * @tpInfo RESTEASY-169
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Html Error")
    public void testHtmlError() throws Exception {
        QuarkusRestWebTarget target = client.target(generateURL("/test"));
        Response response = target.request().header("Accept", "text/html").get();
        String stringResp = response.readEntity(String.class);
        logger.info("response: " + stringResp);
        assertEquals(Status.INTERNAL_SERVER_ERROR, response.getStatus(),
                "The returned response status is not the expected one");
        assertTrue(stringResp.contains("media type: text/html"), "The unexpected error response was thrown");
    }

    /**
     * @tpTestDetails Tests if correct HTTP 406 status code is returned when the specified accept media type
     *                is not supported by the server
     */
    @Test
    @DisplayName("Test Not Acceptable")
    public void testNotAcceptable() throws Exception {
        QuarkusRestWebTarget target = client.target(generateURL("/test/foo"));
        Response response = target.request().header("Accept", "text/plain").get();
        assertEquals(Status.NOT_ACCEPTABLE, response.getStatus(), "The returned response status is not the expected one");
    }

    /**
     * @tpTestDetails Tests of receiving Bad Request response code after html error
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Bad Request After Html Error")
    public void testBadRequestAfterHtmlError() throws Exception {
        QuarkusRestWebTarget target = client.target(generateURL("/test"));
        Response response = target.request().post(Entity.entity("<junk", "application/xml"));
        Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(),
                "The returned response status is not the expected one");
        response.close();
        response = target.request().header("Accept", "text/html").get();
        String stringResp = response.readEntity(String.class);
        logger.info("response: " + stringResp);
        assertEquals(Status.INTERNAL_SERVER_ERROR, response.getStatus(),
                "The returned response status is not the expected one");
        assertTrue(stringResp.contains("media type: text/html"), "The unexpected error response was thrown");
    }
}
