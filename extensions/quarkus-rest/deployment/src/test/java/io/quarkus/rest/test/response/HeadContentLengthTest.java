package io.quarkus.rest.test.response;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.rest.test.response.resource.SimpleResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;

/**
 * @tpSubChapter Parameters
 * @tpChapter Integration tests
 * @tpTestCaseDetails HEAD requests always return non-null Content-Length
 * @tpInfo RESTEASY-1365
 * @tpSince RESTEasy 3.0.19
 * @author Ivo Studensky
 */
@DisplayName("Head Content Length Test")
public class HeadContentLengthTest {

    static Client client;

    @Deployment
    public static Archive<?> deploy() throws Exception {
        WebArchive war = TestUtil.prepareArchive(HeadContentLengthTest.class.getSimpleName());
        return TestUtil.finishContainerPrepare(war, null, SimpleResource.class);
    }

    @BeforeAll
    public static void init() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, HeadContentLengthTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails HEAD requests always return non-null Content-Length
     * @tpSince RESTEasy 3.0.19
     */
    @Test
    @DisplayName("Test Head Content Length")
    public void testHeadContentLength() {
        Builder builder = client.target(generateURL("/simpleresource")).request();
        builder.accept(MediaType.TEXT_PLAIN_TYPE);
        Response getResponse = builder.get();
        String responseBody = getResponse.readEntity(String.class);
        Assertions.assertEquals("hello", responseBody, "The response body doesn't match the expected");
        int getResponseLength = getResponse.getLength();
        Assertions.assertEquals(5, getResponseLength, "The response length doesn't match the expected");
        Response headResponse = builder.head();
        int headResponseLength = headResponse.getLength();
        Assertions.assertEquals(getResponseLength, headResponseLength,
                "The response length from GET and HEAD request doesn't match");
    }
}
