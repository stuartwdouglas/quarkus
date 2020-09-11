package io.quarkus.rest.test.providers.custom;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
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
public class SetRequestUriTest {

    static Client client;

    @BeforeClass
    public static void setup() throws Exception {
        client = ClientBuilder.newClient();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, SetRequestUriResource.class,
                            SetRequestUriRequestFilter.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, SetRequestUriTest.class.getSimpleName());
    }

    @AfterClass
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
    public void testSchemaChange() {
        String uri = generateURL("/base/resource/change");
        String httpsUri = uri.replace("http://", "https://");
        Response response = client.target(uri).request().header("X-Forwarded-Proto", "https").get();
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("The original https uri doesn't match the entity in the response", httpsUri,
                response.readEntity(String.class));
    }

    /**
     * @tpTestDetails Client sends GET request with https protocol uri. The resource has injected UriInfo and returns
     *                response containing absolute path of the uri from the request.
     * @tpPassCrit The response code status is changed to 200 (SUCCESS) and the absolute uri matches the original request
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testUriOverride() {
        Response response = client.target(generateURL("/base/resource/setrequesturi1")).request().get();
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("OK", response.readEntity(String.class));
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
    public void testUriOverride2() {
        Response response = client.target(generateURL("/base/resource/setrequesturi2")).request().get();
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("The original uri doesn't match the entity changed by RequestFilter",
                "http://xx.yy:888/base/resource/sub", response.readEntity(String.class));
    }

}
