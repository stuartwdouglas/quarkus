package io.quarkus.rest.test.core.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.core.basic.resource.ApplicationPropertiesConfig;
import io.quarkus.rest.test.core.basic.resource.ApplicationPropertiesConfigResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Configuration
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for custom Application class with overriden getProperties() method, by injecting Configuration into
 *                    the resource.
 * @tpSince RESTEasy 3.0.16
 */
public class ApplicationPropertiesConfigTest {
    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(ApplicationPropertiesConfig.class, ApplicationPropertiesConfigResource.class);
                    return war;
                }
            });

    @BeforeClass
    public static void init() {
        client = ClientBuilder.newClient();
    }

    @AfterClass
    public static void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ApplicationPropertiesConfigTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test for custom Application class with overriden getProperties() method, by injecting Configuration
     *                into the resource.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testApplicationPropertiesConfig() {
        String errorMessage = "The property is not found in the deployment";
        String response;
        try {
            WebTarget target = client.target(generateURL("/getconfigproperty"));
            response = target.queryParam("prop", "Prop1").request().get(String.class);
        } catch (Exception e) {
            throw new RuntimeException(errorMessage, e);
        }
        Assert.assertEquals(errorMessage, "Value1", response);
    }
}
