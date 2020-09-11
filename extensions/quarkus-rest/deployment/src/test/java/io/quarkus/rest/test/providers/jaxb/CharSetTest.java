package io.quarkus.rest.test.providers.jaxb;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import javax.ws.rs.core.Response.Status;
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
import io.quarkus.rest.test.providers.jaxb.resource.CharSetCustomer;
import io.quarkus.rest.test.providers.jaxb.resource.CharSetResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class CharSetTest {

    private final Logger logger = Logger.getLogger(CharSetResource.class.getName());
    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(CharSetTest.class);
                    return TestUtil.finishContainerPrepare(war, null, CharSetCustomer.class, CharSetResource.class);
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
        return PortProviderUtil.generateURL(path, CharSetTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Client sends POST request with jaxb annotated object entity and the targeted resource receives jaxb
     *                object with corect encoding.
     * @tpPassCrit The jaxb object xml element is same as original
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testReceiveJaxbObjectAsItis() throws Exception {
        ResteasyWebTarget target = client.target(generateURL("/test/string"));
        CharSetCustomer cust = new CharSetCustomer();
        String name = "bill\u00E9";
        cust.setName(name);
        Response response = target.request().accept("application/xml")
                .post(Entity.entity(cust, MediaType.APPLICATION_XML_TYPE));
        Assert.assertEquals(Status.OK, response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Client sends POST request with jaxb annotated object entity and the targeted resource receives
     *                xml string.
     * @tpPassCrit Jaxb object is unmarshalled to the expected xml string with correct encoding
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testReceiveJaxbObjectAsString() throws Exception {
        ResteasyWebTarget target = client.target(generateURL("/test"));
        CharSetCustomer cust = new CharSetCustomer();
        String name = "bill\u00E9";
        logger.info("client name: " + name);
        logger.info("bytes string: " + new String(name.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
        cust.setName(name);
        Response response = target.request().accept("application/xml")
                .post(Entity.entity(cust, MediaType.APPLICATION_XML_TYPE));
        Assert.assertEquals(Status.OK, response.getStatus());
        response.close();
    }
}
