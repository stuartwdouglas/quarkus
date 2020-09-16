package io.quarkus.rest.test.validation;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.rest.test.validation.resource.ManagedBeanValidationResource;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Verify class / property validation occurs for JNDI resources.
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1203
 * @tpSince RESTEasy 3.12.0
 */
public class ManagedBeanValidationTest {

    private static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
                    return TestUtil.finishContainerPrepare(war, null, ManagedBeanValidationResource.class);
                }
            });

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ManagedBeanValidationTest.class.getSimpleName());
    }

    @BeforeClass
    public static void beforeClass() {
        client = ClientBuilder.newClient();
    }

    @AfterClass
    public static void afterClass() {
        client.close();
    }

    /**
     * @tpTestDetails Verify behavior for valid parameters.
     * @tpSince RESTEasy 3.12.0
     */
    @Test
    public void testManagedBeanValidationValid() {
        Response response = client.target(generateURL("/validate")).queryParam("q", 2).request().get();
        Assert.assertEquals(200, response.getStatus());
        response = client.target(generateURL("/visited")).request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(response.readEntity(boolean.class));
    }

    /**
     * @tpTestDetails Verify behavior for invalid parameters.
     * @tpSince RESTEasy 3.12.0
     */
    @Test
    public void testManagedBeanValidationInvalid() {
        Response response = client.target(generateURL("/validate")).queryParam("q", 0).request().get();
        Assert.assertEquals(400, response.getStatus());
        response = client.target(generateURL("/visited")).request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertFalse(response.readEntity(boolean.class));
    }
}
