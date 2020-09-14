package io.quarkus.rest.test.providers.jaxb;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.jaxb.resource.QualityFactorResource;
import io.quarkus.rest.test.providers.jaxb.resource.QualityFactorThing;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class QualityFactorTest {

    static QuarkusRestClient client;
    private static Logger logger = Logger.getLogger(QualityFactorTest.class.getName());

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(JaxbCollectionTest.class);
                    return TestUtil.finishContainerPrepare(war, null, QualityFactorResource.class, QualityFactorThing.class);
                }
            });

    @Before
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, QualityFactorTest.class.getSimpleName());
    }

    @Test
    public void testHeader() throws Exception {
        Response response = client.target(generateURL("/test")).request()
                .accept("application/xml; q=0.5", "application/json; q=0.8").get();
        String result = response.readEntity(String.class);
        logger.info(result);
        Assert.assertTrue("The format of the response doesn't reflect the quality factor", result.startsWith("{"));

    }
}
