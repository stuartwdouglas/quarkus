package io.quarkus.rest.test.providers.jaxb;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;

import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
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
public class XmlHeaderTest {

    private final Logger logger = Logger.getLogger(XmlHeaderTest.class.getName());
    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, XmlHeaderResource.class, XmlHeaderDecorator.class,
                            XmlHeaderDecorator2.class, XmlHeaderJunk2Intf.class, XmlHeaderJunkIntf.class, XmlHeaderThing.class);
                }
            });

    @Before
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @After
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
    public void testHeader() throws Exception {
        ResteasyWebTarget target = client.target(generateURL("/test/header"));
        String response = target.request().get(String.class);
        logger.info(response);
        Assert.assertTrue("The response doesn't contain the expected xml-stylesheet header",
                response.contains("<?xml-stylesheet"));

    }

    /**
     * @tpTestDetails This tests decorators in general with the @Stylesheet annotation
     * @tpPassCrit The response contains expected xml-stylesheet header
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testStylesheet() throws Exception {
        ResteasyWebTarget target = client.target(generateURL("/test/stylesheet"));
        String response = target.request().get(String.class);
        logger.info(response);
        Assert.assertTrue("The response doesn't contain the expected xml-stylesheet header",
                response.contains("<?xml-stylesheet"));

    }

}
