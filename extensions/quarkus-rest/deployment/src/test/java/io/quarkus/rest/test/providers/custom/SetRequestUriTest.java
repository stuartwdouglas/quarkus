package io.quarkus.rest.test.providers.custom;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
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

import io.quarkus.rest.test.providers.custom.resource.SetRequestUriRequestFilter;
import io.quarkus.rest.test.providers.custom.resource.SetRequestUriResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Set Request Uri Test")
public class SetRequestUriTest {

    static Client client;

    @BeforeAll
    public static void setup() throws Exception {
        client = ClientBuilder.newClient();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, SetRequestUriResource.class, SetRequestUriRequestFilter.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, SetRequestUriTest.class.getSimpleName());
    }

    @AfterAll
    public static void close() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Client sends GET request with https protocol uri. The resource has injected UriInfo and returns
     *                response containing absolute path of the uri from the request.
     * @tpPassCrit The response code status is changed to 200 (SUCCESS) and the absolute uri matches the original request
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Schema Change")
    public void testSchemaChange() {
        String uri = generateURL("/base/resource/change");
        String httpsUri = uri.replace("http://", "https://");
        Response response = client.target(uri).request().header("X-Forwarded-Proto", "https").get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(httpsUri, response.readEntity(String.class),
                "The original https uri doesn't match the entity in the response");
    }

    /**
     * @tpTestDetails Client sends GET request with https protocol uri. The resource has injected UriInfo and returns
     *                response containing absolute path of the uri from the request.
     * @tpPassCrit The response code status is changed to 200 (SUCCESS) and the absolute uri matches the original request
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Uri Override")
    public void testUriOverride() {
        Response response = client.target(generateURL("/base/resource/setrequesturi1")).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "OK");
    }

    /**
     * @tpTestDetails Client sends GET request with non existing uri path. The request is catched by PreMatching
     *                RequestFilter which applies to all requests not matter if the resource exists on the server. The
     *                RequestFilter
     *                processes the request aborts processing of the request and sends response back to the client.
     * @tpPassCrit The response code status is 200 (SUCCESS) and the absolute uri is changed by RequestFilter
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Uri Override 2")
    public void testUriOverride2() {
        Response response = client.target(generateURL("/base/resource/setrequesturi2")).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("http://xx.yy:888/base/resource/sub", response.readEntity(String.class),
                "The original uri doesn't match the entity changed by RequestFilter");
    }
}
