package io.quarkus.rest.test.providers.jaxb;

import java.io.StringWriter;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.xml.bind.JAXBContext;

import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.jaxb.resource.SeeAlsoAnnotationBaseFoo;
import io.quarkus.rest.test.providers.jaxb.resource.SeeAlsoAnnotationFooIntf;
import io.quarkus.rest.test.providers.jaxb.resource.SeeAlsoAnnotationRealFoo;
import io.quarkus.rest.test.providers.jaxb.resource.SeeAlsoAnnotationResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class SeeAlsoAnnotationTest {

    private final Logger logger = Logger.getLogger(SeeAlsoAnnotationTest.class.getName());
    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, SeeAlsoAnnotationResource.class,
                            SeeAlsoAnnotationRealFoo.class,
                            SeeAlsoAnnotationBaseFoo.class, SeeAlsoAnnotationFooIntf.class);
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
        return PortProviderUtil.generateURL(path, SeeAlsoAnnotationTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Tests jaxb @SeeAlsoAnnotation
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testIntf() throws Exception {
        String url = generateURL("/see/intf");
        runTest(url);
    }

    /**
     * @tpTestDetails Tests jaxb @SeeAlsoAnnotation
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testTest() throws Exception {
        String url = generateURL("/see/base");
        runTest(url);
    }

    private void runTest(String url) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(SeeAlsoAnnotationRealFoo.class);
        StringWriter writer = new StringWriter();
        SeeAlsoAnnotationRealFoo foo = new SeeAlsoAnnotationRealFoo();
        foo.setName("bill");

        ctx.createMarshaller().marshal(foo, writer);

        String s = writer.getBuffer().toString();
        logger.info(s);

        ResteasyWebTarget target = client.target(generateURL(url));
        target.request().header("Content-Type", "application/xml").put(Entity.xml(s));
    }

}
