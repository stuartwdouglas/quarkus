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
import io.quarkus.rest.test.providers.jaxb.resource.XmlHeaderDecorator;
import io.quarkus.rest.test.providers.jaxb.resource.XmlHeaderDecorator2;
import io.quarkus.rest.test.providers.jaxb.resource.XmlHeaderJunk2Intf;
import io.quarkus.rest.test.providers.jaxb.resource.XmlHeaderJunkIntf;
import io.quarkus.rest.test.providers.jaxb.resource.XmlHeaderResource;
import io.quarkus.rest.test.providers.jaxb.resource.XmlHeaderThing;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Xml Header Test")
public class XmlHeaderTest {

    private final Logger logger = Logger.getLogger(XmlHeaderTest.class.getName());

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, XmlHeaderResource.class, XmlHeaderDecorator.class,
                    XmlHeaderDecorator2.class, XmlHeaderJunk2Intf.class, XmlHeaderJunkIntf.class, XmlHeaderThing.class);
        }
    });

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
        client = null;
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, XmlHeaderTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails This tests decorators in general with the @XmlHeader annotation
     * @tpPassCrit The response contains expected xml-stylesheet header
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Header")
    public void testHeader() throws Exception {
        QuarkusRestWebTarget target = client.target(generateURL("/test/header"));
        String response = target.request().get(String.class);
        logger.info(response);
        Assertions.assertTrue(response.contains("<?xml-stylesheet"),
                "The response doesn't contain the expected xml-stylesheet header");
    }

    /**
     * @tpTestDetails This tests decorators in general with the @Stylesheet annotation
     * @tpPassCrit The response contains expected xml-stylesheet header
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Stylesheet")
    public void testStylesheet() throws Exception {
        QuarkusRestWebTarget target = client.target(generateURL("/test/stylesheet"));
        String response = target.request().get(String.class);
        logger.info(response);
        Assertions.assertTrue(response.contains("<?xml-stylesheet"),
                "The response doesn't contain the expected xml-stylesheet header");
    }
}
