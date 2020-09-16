package io.quarkus.rest.test.core.encoding;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.core.encoding.resource.MatrixParamEncodingResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Encoding
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-729
 * @tpSince RESTEasy 3.0.16
 */
public class MatrixParamEncodingTest {

    protected static final Logger logger = Logger.getLogger(MatrixParamEncodingTest.class.getName());

    protected static QuarkusRestClient client;

    @Before
    public void setup() throws Exception {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, MatrixParamEncodingTest.class.getSimpleName());
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, MatrixParamEncodingResource.class);
                }
            });

    @After
    public void shutdown() throws Exception {
        client.close();
        client = null;
    }

    /**
     * @tpTestDetails Check decoded request, do not use UriBuilder
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testMatrixParamRequestDecoded() throws Exception {
        QuarkusRestWebTarget target = client.target(generateURL("/decoded")).matrixParam("param", "ac/dc");
        Response response = target.request().get();
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals("Wrong response", "ac/dc", response.readEntity(String.class));
        response.close();
    }

    /**
     * @tpTestDetails Check decoded request, one matrix param is not defined, do not use UriBuilder
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testMatrixParamNullRequestDecoded() throws Exception {
        QuarkusRestWebTarget target = client.target(generateURL("/decodedMultipleParam")).matrixParam("param1", "")
                .matrixParam("param2", "abc");
        Response response = target.request().get();
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals("Wrong response", "null abc", response.readEntity(String.class));
        response.close();
    }

    /**
     * @tpTestDetails Check encoded request, do not use UriBuilder
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testMatrixParamRequestEncoded() throws Exception {
        QuarkusRestWebTarget target = client.target(generateURL("/encoded")).matrixParam("param", "ac/dc");
        Response response = target.request().get();
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals("Wrong response", "ac%2Fdc", response.readEntity(String.class));
        response.close();
    }

    /**
     * @tpTestDetails Check decoded request, use UriBuilder
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testMatrixParamUriBuilderDecoded() throws Exception {
        UriBuilder uriBuilder = UriBuilder.fromUri(generateURL("/decoded"));
        uriBuilder.matrixParam("param", "ac/dc");
        QuarkusRestWebTarget target = client.target(uriBuilder.build().toString());
        logger.info("Sending request to " + uriBuilder.build().toString());
        Response response = target.request().get();
        String entity = response.readEntity(String.class);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals("Wrong response", "ac/dc", entity);
        response.close();
    }

    /**
     * @tpTestDetails Check encoded request, use UriBuilder
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testMatrixParamUriBuilderEncoded() throws Exception {
        UriBuilder uriBuilder = UriBuilder.fromUri(generateURL("/encoded"));
        uriBuilder.matrixParam("param", "ac/dc");
        QuarkusRestWebTarget target = client.target(uriBuilder.build().toString());
        logger.info("Sending request to " + uriBuilder.build().toString());
        Response response = target.request().get();
        String entity = response.readEntity(String.class);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals("Wrong response", "ac%2Fdc", entity);
        response.close();
    }
}
