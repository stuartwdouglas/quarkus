package io.quarkus.rest.test.providers.jaxb;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;

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
import io.quarkus.rest.test.providers.jaxb.resource.XmlEnumParamLocation;
import io.quarkus.rest.test.providers.jaxb.resource.XmlEnumParamResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Xml Enum Param Test")
public class XmlEnumParamTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(XmlEnumParamTest.class);
            return TestUtil.finishContainerPrepare(war, null, XmlEnumParamResource.class, XmlEnumParamLocation.class);
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
        return PortProviderUtil.generateURL(path, XmlEnumParamTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Tests xml enum param in the resource
     * @tpPassCrit The expected enum type is returned
     * @tpInfo RESTEASY-428
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Xml Enum Param")
    public void testXmlEnumParam() throws Exception {
        QuarkusRestWebTarget target = client.target(generateURL("/enum"));
        String response = target.queryParam("loc", "north").request().get(String.class);
        Assertions.assertEquals("NORTH", response.toUpperCase(), "The response doesn't contain expected enum type");
    }
}
