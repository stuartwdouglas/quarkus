package io.quarkus.rest.test.resource.path;

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

import io.quarkus.rest.test.resource.path.resource.WildcardMatchingResource;
import io.quarkus.rest.test.resource.path.resource.WildcardMatchingSubResource;
import io.quarkus.rest.test.resource.path.resource.WildcardMatchingSubSubResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpTestCaseDetails Check class name of sub-resources, which process client request
 * @tpSince RESTEasy 3.0.16
 */
public class WildcardResourceMatchingTest {

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, WildcardMatchingResource.class,
                            WildcardMatchingSubResource.class, WildcardMatchingSubSubResource.class);
                }
            });

    @BeforeClass
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterClass
    public static void cleanup() {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, WildcardResourceMatchingTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Check root resource.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testMain() {
        Response response = client.target(generateURL("/main")).request().get();
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("WildcardMatchingResource", response.readEntity(String.class));
        response.close();
    }

    /**
     * @tpTestDetails Check sub-resource.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testMainSub() {
        Response response = client.target(generateURL("/main/sub")).request().get();
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("WildcardMatchingSubResource", response.readEntity(String.class));
        response.close();
    }

    /**
     * @tpTestDetails Check sub-sub-resource.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testMainSubSub() {
        Response response = client.target(generateURL("/main/sub/sub")).request().get();
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("WildcardMatchingSubSubResource", response.readEntity(String.class));
        response.close();
    }

}
