package io.quarkus.rest.test.providers.jaxb;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;

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
public class XmlEnumParamTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(XmlEnumParamTest.class);
                    return TestUtil.finishContainerPrepare(war, null, XmlEnumParamResource.class, XmlEnumParamLocation.class);
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
        return PortProviderUtil.generateURL(path, XmlEnumParamTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Tests xml enum param in the resource
     * @tpPassCrit The expected enum type is returned
     * @tpInfo RESTEASY-428
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testXmlEnumParam() throws Exception {
        ResteasyWebTarget target = client.target(generateURL("/enum"));
        String response = target.queryParam("loc", "north").request().get(String.class);
        Assert.assertEquals("The response doesn't contain expected enum type", "NORTH", response.toUpperCase());
    }

}
