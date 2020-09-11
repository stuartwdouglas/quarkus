package io.quarkus.rest.test.resource.path;

import java.util.function.Supplier;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.resource.path.resource.ResourceLocatorWithBaseNoExpressionResource;
import io.quarkus.rest.test.resource.path.resource.ResourceLocatorWithBaseNoExpressionSubresource;
import io.quarkus.rest.test.resource.path.resource.ResourceLocatorWithBaseNoExpressionSubresource2;
import io.quarkus.rest.test.resource.path.resource.ResourceLocatorWithBaseNoExpressionSubresource3;
import io.quarkus.rest.test.resource.path.resource.ResourceLocatorWithBaseNoExpressionSubresource3Interface;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpTestCaseDetails Check resources with locator with no expression
 * @tpSince RESTEasy 3.0.16
 */
public class ResourceLocatorWithBaseNoExpressionTest {
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

                    war.addClasses(ResourceLocatorWithBaseNoExpressionSubresource.class,
                            ResourceLocatorWithBaseNoExpressionSubresource2.class,
                            ResourceLocatorWithBaseNoExpressionSubresource3.class,
                            ResourceLocatorWithBaseNoExpressionSubresource3Interface.class);
                    return TestUtil.finishContainerPrepare(war, null, ResourceLocatorWithBaseNoExpressionResource.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ResourceLocatorWithBaseNoExpressionTest.class.getSimpleName());
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
            Assert.assertEquals(Status.OK, response.getStatus());
            Assert.assertEquals(ERROR_MSG, ResourceLocatorWithBaseNoExpressionSubresource.class.getName(),
                    response.readEntity(String.class));
            response.close();
        }
        {
            Response response = client.target(generateURL("/a1/base/1/resources/subresource2/stuff/2/bar")).request().get();
            Assert.assertEquals(Status.OK, response.getStatus());
            Assert.assertEquals(ERROR_MSG, ResourceLocatorWithBaseNoExpressionSubresource2.class.getName() + "-2",
                    response.readEntity(String.class));
            response.close();
        }
    }

}
