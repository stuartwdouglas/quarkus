package io.quarkus.rest.test.client.exception;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.plugins.delegates.MediaTypeHeaderDelegate;
import org.jboss.resteasy.util.HttpHeaderNames;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.client.exception.resource.ClientErrorResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpSince RESTEasy 3.0.20
 * @tpTestCaseDetails Test client error caused by bad media type
 */
@DisplayName("Client Error Test")
public class ClientErrorTest {

    private static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(PortProviderUtil.class);
            war.addClass(TestUtil.class);
            return TestUtil.finishContainerPrepare(war, null, ClientErrorResource.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ClientErrorTest.class.getSimpleName());
    }

    @BeforeAll
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails There are two methods that match path, but only one matches Accept.
     * @tpSince RESTEasy 3.0.20
     */
    @Test
    @DisplayName("Test Complex")
    public void testComplex() {
        Builder builder = client.target(generateURL("/complex/match")).request();
        builder.header(HttpHeaderNames.ACCEPT, "text/xml");
        Response response = null;
        try {
            response = builder.get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            response.close();
        }
    }

    /**
     * @tpTestDetails No method matches path.
     * @tpSince RESTEasy 3.0.20
     */
    @Test
    @DisplayName("Test Not Found")
    public void testNotFound() {
        Builder builder = client.target(generateURL("/foo/notthere")).request();
        builder.header(HttpHeaderNames.ACCEPT, "application/foo");
        Response response = null;
        try {
            response = builder.get();
            Assertions.assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            response.close();
        }
    }

    /**
     * @tpTestDetails Two methods match path, but neither matches HTTP method.
     * @tpSince RESTEasy 3.0.20
     */
    @Test
    @DisplayName("Test Method Not Allowed")
    public void testMethodNotAllowed() {
        Builder builder = client.target(generateURL("")).request();
        builder.header(HttpHeaderNames.ACCEPT, "application/foo");
        Response response = null;
        try {
            response = builder.get();
            Assertions.assertEquals(Status.METHOD_NOT_ALLOWED.getStatusCode(), response.getStatus());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            response.close();
        }
    }

    /**
     * @tpTestDetails There is a method that matches path but does not match Accept.
     * @tpSince RESTEasy 3.0.20
     */
    @Test
    @DisplayName("Test Not Acceptable")
    public void testNotAcceptable() {
        Builder builder = client.target(generateURL("")).request();
        builder.header(HttpHeaderNames.ACCEPT, "application/bar");
        Response response = null;
        try {
            response = builder.post(Entity.entity("content", "application/bar"));
            Assertions.assertEquals(Status.NOT_ACCEPTABLE.getStatusCode(), response.getStatus());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            response.close();
        }
    }

    /**
     * @tpTestDetails Matching POST method returns no entity.
     * @tpSince RESTEasy 3.0.20
     */
    @Test
    @DisplayName("Test No Content Post")
    public void testNoContentPost() {
        Builder builder = client.target(generateURL("/nocontent")).request();
        Response response = null;
        try {
            response = builder.post(Entity.entity("content", "text/plain"));
            Assertions.assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            response.close();
        }
    }

    /**
     * @tpTestDetails Matching DELETE returns no entity.
     * @tpSince RESTEasy 3.0.20
     */
    @Test
    @DisplayName("Test No Content")
    public void testNoContent() {
        Builder builder = client.target(generateURL("")).request();
        Response response = null;
        try {
            response = builder.delete();
            Assertions.assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            response.close();
        }
    }

    /**
     * @tpTestDetails Method matches path but does not match content type.
     * @tpSince RESTEasy 3.0.20
     */
    @Test
    @DisplayName("Test Unsupported Media Type")
    public void testUnsupportedMediaType() {
        Builder builder = client.target(generateURL("")).request();
        builder.header(HttpHeaderNames.ACCEPT, "application/foo");
        Response response = null;
        try {
            response = builder.post(Entity.entity("content", "text/plain"));
            Assertions.assertEquals(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode(), response.getStatus());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            response.close();
        }
    }

    /**
     * @tpTestDetails Method matches path but not bad Accept media type with no subtype.
     * @tpSince RESTEasy 3.0.20
     */
    @Test
    @DisplayName("Test Bad Accept Media Type No Sub Type")
    public void testBadAcceptMediaTypeNoSubType() {
        Builder builder = client.target(generateURL("/complex/match")).request();
        builder.header(HttpHeaderNames.ACCEPT, "text");
        Response response = null;
        try {
            response = builder.get();
            Assertions.assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            response.close();
        }
    }

    /**
     * @tpTestDetails Method matches path but not bad Accept media type with nonnumerical q value.
     * @tpSince RESTEasy 3.0.20
     */
    @Test
    @DisplayName("Test Bad Accept Media Type Non Numeric Quality Value")
    public void testBadAcceptMediaTypeNonNumericQualityValue() {
        Builder builder = client.target(generateURL("/complex/match")).request();
        builder.header(HttpHeaderNames.ACCEPT, "text/plain; q=bad");
        Response response = null;
        try {
            response = builder.get();
            Assertions.assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            response.close();
        }
    }

    @DisplayName("Test Media Type Header Delegate")
    static class TestMediaTypeHeaderDelegate extends MediaTypeHeaderDelegate {

        public static MediaType parse(String type) {
            if ("text".equals(type)) {
                return new MediaType("text", "");
            }
            return MediaTypeHeaderDelegate.parse(type);
        }
    }
}
