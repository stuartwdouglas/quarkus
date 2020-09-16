package io.quarkus.rest.test.providers.jaxb;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.providers.jaxb.resource.CustomOverrideFoo;
import io.quarkus.rest.test.providers.jaxb.resource.CustomOverrideResource;
import io.quarkus.rest.test.providers.jaxb.resource.CustomOverrideWriter;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Custom Override Test")
public class CustomOverrideTest {

    private static Logger logger = Logger.getLogger(CustomOverrideTest.class.getName());

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, CustomOverrideResource.class, CustomOverrideWriter.class,
                    CustomOverrideFoo.class);
        }
    });

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, CustomOverrideTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test for same resource path for media type xml and "text/x-vcard" with custom MessageBodyWriter
     * @tpInfo RESTEASY-510
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Regression")
    public void testRegression() throws Exception {
        QuarkusRestWebTarget target = client.target(generateURL("/test"));
        String response = target.request().accept("text/x-vcard").get(String.class);
        logger.info(response);
        Assertions.assertEquals(response, "---bill---");
        response = target.request().accept("application/xml").get(String.class);
        Assertions.assertTrue(response.contains("customOverrideFoo"));
        logger.info(response);
    }
}
