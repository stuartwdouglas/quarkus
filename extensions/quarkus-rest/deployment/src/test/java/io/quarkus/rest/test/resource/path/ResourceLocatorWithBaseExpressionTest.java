package io.quarkus.rest.test.resource.path;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.resource.path.resource.ResourceLocatorWithBaseExpressionResource;
import io.quarkus.rest.test.resource.path.resource.ResourceLocatorWithBaseExpressionSubresource;
import io.quarkus.rest.test.resource.path.resource.ResourceLocatorWithBaseExpressionSubresource2;
import io.quarkus.rest.test.resource.path.resource.ResourceLocatorWithBaseExpressionSubresource3;
import io.quarkus.rest.test.resource.path.resource.ResourceLocatorWithBaseExpressionSubresource3Interface;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpTestCaseDetails Check resources with locator with base expression
 * @tpSince RESTEasy 3.0.16
 */
public class ResourceLocatorWithBaseExpressionTest {
    private static final String ERROR_MSG = "Response contain wrong content";
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

                    war.addClasses(ResourceLocatorWithBaseExpressionSubresource.class,
                            ResourceLocatorWithBaseExpressionSubresource2.class,
                            ResourceLocatorWithBaseExpressionSubresource3.class,
                            ResourceLocatorWithBaseExpressionSubresource3Interface.class);
                    return TestUtil.finishContainerPrepare(war, null, ResourceLocatorWithBaseExpressionResource.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ResourceLocatorWithBaseExpressionTest.class.getSimpleName());
    }

    @AfterClass
    public static void close() throws Exception {
        client.close();
    }

    @AfterClass
    public static void after() throws Exception {

    }

    /**
     * @tpTestDetails Test for root resource and for subresource.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testSubresource() throws Exception {
        {
            Response response = client.target(generateURL("/a1/base/1/resources")).request().get();
            Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assert.assertEquals(ERROR_MSG, ResourceLocatorWithBaseExpressionSubresource.class.getName(),
                    response.readEntity(String.class));
            response.close();
        }
        {
            Response response = client.target(generateURL("/a1/base/1/resources/subresource2/stuff/2/bar")).request().get();
            Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assert.assertEquals(ERROR_MSG, ResourceLocatorWithBaseExpressionSubresource2.class.getName() + "-2",
                    response.readEntity(String.class));
            response.close();
        }
    }

}
