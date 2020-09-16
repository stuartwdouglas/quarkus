package io.quarkus.rest.test.resource.param;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.Assert;
import io.quarkus.rest.test.resource.param.resource.MultiplePathSegmentResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Parameters
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for @PathParam capturing multiple PathSegments (RESTEASY-1653)
 * @tpSince RESTEasy 3.1.3.Final
 */
public class MultiplePathSegmentTest {

    private static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, MultiplePathSegmentResource.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, MultiplePathSegmentTest.class.getSimpleName());
    }

    @BeforeAll
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void cleanup() {
        client.close();
    }

    /**
     * @tpTestDetails Array of PathSegments captured by wildcard
     * @tpSince RESTEasy 3.1.3.Final
     */
    @Test
    public void testWildcardArray() throws Exception {
        Invocation.Builder request = client.target(generateURL("/a/b/c/array/3")).request();
        Response response = request.get();
        Assert.assertEquals(200, response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails List of PathSegments captured by wildcard
     * @tpSince RESTEasy 3.1.3.Final
     */
    @Test
    public void testWildcardList() throws Exception {
        Invocation.Builder request = client.target(generateURL("/a/b/c/list/3")).request();
        Response response = request.get();
        Assert.assertEquals(200, response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails ArrayList of PathSegments captured by wildcard
     * @tpSince RESTEasy 3.1.3.Final
     */
    @Test
    public void testWildcardArrayList() throws Exception {
        Invocation.Builder request = client.target(generateURL("/a/b/c/arraylist/3")).request();
        Response response = request.get();
        Assert.assertEquals(200, response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Array of PathSegments captured by two separate segments with the same name
     * @tpSince RESTEasy 3.1.3.Final
     */
    @Test
    public void testTwoSegmentsArray() throws Exception {
        Invocation.Builder request = client.target(generateURL("/a/b/array")).request();
        Response response = request.get();
        Assert.assertEquals(200, response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails List of PathSegments captured by two separate segments with the same name
     * @tpSince RESTEasy 3.1.3.Final
     */
    @Test
    public void testTwoSegmentsList() throws Exception {
        Invocation.Builder request = client.target(generateURL("/a/b/list")).request();
        Response response = request.get();
        Assert.assertEquals(200, response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails ArrayList of PathSegments captured by two separate segments with the same name
     * @tpSince RESTEasy 3.1.3.Final
     */
    @Test
    public void testTwoSegmentsArrayList() throws Exception {
        Invocation.Builder request = client.target(generateURL("/a/b/arraylist")).request();
        Response response = request.get();
        Assert.assertEquals(200, response.getStatus());
        response.close();
    }
}
