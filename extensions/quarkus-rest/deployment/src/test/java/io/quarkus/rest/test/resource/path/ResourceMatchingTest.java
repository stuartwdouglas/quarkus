package io.quarkus.rest.test.resource.path;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.resource.path.resource.ResourceMatchingAnotherResourceLocator;
import io.quarkus.rest.test.resource.path.resource.ResourceMatchingAnotherSubResource;
import io.quarkus.rest.test.resource.path.resource.ResourceMatchingErrorResource;
import io.quarkus.rest.test.resource.path.resource.ResourceMatchingMainSubResource;
import io.quarkus.rest.test.resource.path.resource.ResourceMatchingMediaWriter;
import io.quarkus.rest.test.resource.path.resource.ResourceMatchingNoMediaResource;
import io.quarkus.rest.test.resource.path.resource.ResourceMatchingStringBean;
import io.quarkus.rest.test.resource.path.resource.ResourceMatchingStringBeanEntityProvider;
import io.quarkus.rest.test.resource.path.resource.ResourceMatchingWeightResource;
import io.quarkus.rest.test.resource.path.resource.ResourceMatchingYetAnotherSubresource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Resource Matching Test")
public class ResourceMatchingTest {

    public static final String readFromStream(InputStream stream) throws IOException {
        InputStreamReader isr = new InputStreamReader(stream);
        return readFromReader(isr);
    }

    public static final String readFromReader(Reader reader) throws IOException {
        BufferedReader br = new BufferedReader(reader);
        String entity = br.readLine();
        br.close();
        return entity;
    }

    static Client client;

    @BeforeAll
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(ResourceMatchingStringBean.class, ResourceMatchingAnotherResourceLocator.class);
            return TestUtil.finishContainerPrepare(war, null, ResourceMatchingYetAnotherSubresource.class,
                    ResourceMatchingMainSubResource.class, ResourceMatchingAnotherSubResource.class,
                    ResourceMatchingWeightResource.class, ResourceMatchingErrorResource.class,
                    ResourceMatchingNoMediaResource.class, ResourceMatchingStringBeanEntityProvider.class,
                    ResourceMatchingMediaWriter.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ResourceMatchingTest.class.getSimpleName());
    }

    @AfterAll
    public static void close() {
        client.close();
    }

    /**
     * @tpTestDetails Client sends GET request, the returned response Mediatype is APPLICATION_SVG_XML_TYPE as set up
     *                by the custom MessageBodyWriter.
     * @tpPassCrit The correct mediatype and response code 200 (Success) is returned
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Media Type From Provider")
    public void testMediaTypeFromProvider() {
        Response response = client.target(generateURL("/nomedia/list")).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(MediaType.APPLICATION_SVG_XML_TYPE, response.getMediaType());
        response.close();
    }

    /**
     * @tpTestDetails Client sends GET request, the returned response Mediatype is APPLICATION_OCTET_STREAM_TYPE as
     *                set up by the custom MessageBodyWriter.
     * @tpPassCrit The correct mediatype and response code 200 (Success) is returned
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test No Produces")
    public void testNoProduces() {
        Response response = client.target(generateURL("/nomedia/nothing")).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(MediaType.APPLICATION_OCTET_STREAM_TYPE, response.getMediaType());
        response.close();
    }

    /**
     * @tpTestDetails Client sends GET request with request header of media type "text/*". The matching resource path
     *                doesn't accept this mediatype. According to HTTP 1.1 specification:
     *                "HTTP/1.1 servers are allowed to return responses which are not acceptable according to the accept headers
     *                sent in the request."
     * @tpPassCrit The response code 406 (Not Acceptable) is returned
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Non Concrete Match")
    public void testNonConcreteMatch() {
        Response response = client.target(generateURL("/error")).request("text/*").get();
        Assertions.assertEquals(Status.NOT_ACCEPTABLE.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Client sends GET request, with header accept type "testi/*", the more specific resource is chosen
     * @tpPassCrit The response code 200 (Success) is returned and the correct resource is chosen
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Wildcard")
    public void testWildcard() {
        Response response = client.target(generateURL("/yas")).request("testi/*").get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "test/text");
        response.close();
    }

    /**
     * @tpTestDetails Client sends GET request, with header accept type "testiii/textiii" and "application/xml",
     *                "application/xml" media type has in the resource higher probability to be chosen (indicates that by higher
     *                "qs"
     *                than other resources)
     * @tpPassCrit The returned resource is the resource with media type "application/xml"
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test QS")
    public void testQS() {
        Response response = client.target(generateURL("/yas")).request("testiii/textiii", "application/xml").get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "application/xml");
        response.close();
    }

    /**
     * @tpTestDetails Client sends empty POST request, with specified header Content-Type "text/plain",
     * @tpPassCrit The correct subresource is chosen
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Override")
    public void testOverride() {
        Response response = client.target(generateURL("/resource/subresource/sub")).request()
                .header("Content-Type", "text/plain").post(null);
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(ResourceMatchingAnotherSubResource.class.getSimpleName(), response.readEntity(String.class),
                "The incorrect subresource was chosen");
        response.close();
    }

    /**
     * @tpTestDetails Client sends OPTIONS request, which allows it to determine which HTTP methods it can issue without
     *                initiating resource retrieval.
     * @tpPassCrit The response code 200 (Success) is returned and response contains GET option.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Options")
    public void testOptions() {
        Response response = client.target(generateURL("/resource/subresource/something")).request().options();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String actual = response.readEntity(String.class);
        Assertions.assertTrue(actual.contains("GET"), "GET is not in the list of available methods for the specified resource");
        response.close();
    }

    /**
     * @tpTestDetails Client sends empty POST request with mediatype q parameter specified. It gives higher weight (q=0.9)
     *                to a mediatype "application/*" then to mediatype "application/xml" (q=0.1).
     * @tpPassCrit The more specific mediatype will be chosen by the server ("application/xml")
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Avoid Wildcard")
    public void testAvoidWildcard() {
        Response response = client.target(generateURL("/weight")).request("application/*;q=0.9", "application/xml;q=0.1")
                .post(null);
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        MediaType mediaType = response.getMediaType();
        String actual = response.readEntity(String.class);
        Assertions.assertEquals(actual, "application/xml");
        Assertions.assertEquals("application/xml;charset=UTF-8", mediaType.toString(),
                "MediaType of the response doesn't match the expected type");
        response.close();
    }
}
