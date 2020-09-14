package io.quarkus.rest.test.providers.jackson2;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.jackson2.resource.JacksonJaxbCoexistenceJacksonResource;
import io.quarkus.rest.test.providers.jackson2.resource.JacksonJaxbCoexistenceJacksonXmlResource;
import io.quarkus.rest.test.providers.jackson2.resource.JacksonJaxbCoexistenceProduct2;
import io.quarkus.rest.test.providers.jackson2.resource.JacksonJaxbCoexistenceXmlProduct;
import io.quarkus.rest.test.providers.jackson2.resource.JacksonJaxbCoexistenceXmlResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jackson2 provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@Ignore
public class JacksonJaxbCoexistenceTest {

    static QuarkusRestClient client;
    protected static final Logger logger = Logger.getLogger(JacksonJaxbCoexistenceTest.class.getName());

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, JacksonJaxbCoexistenceJacksonResource.class,
                            JacksonJaxbCoexistenceJacksonXmlResource.class, JacksonJaxbCoexistenceXmlResource.class,
                            JacksonJaxbCoexistenceProduct2.class,
                            JacksonJaxbCoexistenceXmlProduct.class);
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
        return PortProviderUtil.generateURL(path, JacksonJaxbCoexistenceTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Client sends GET request for json annotated resource. In the first case it returns single json entity,
     *                in the second case multiple json entities as String.
     * @tpPassCrit The resource returns json entities in correct format
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testJacksonString() throws Exception {
        WebTarget target = client.target(generateURL("/products/333"));
        Response response = target.request().get();
        String entity = response.readEntity(String.class);
        logger.info(entity);
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("The response entity content doesn't match the expected",
                "{\"name\":\"Iphone\",\"id\":333}", entity);

        response.close();

        target = client.target(generateURL("/products"));
        response = target.request().get();
        entity = response.readEntity(String.class);
        logger.info(entity);
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("The response entity content doesn't match the expected",
                "[{\"name\":\"Iphone\",\"id\":333},{\"name\":\"macbook\",\"id\":44}]", entity);
        response.close();
    }

    /**
     * @tpTestDetails Test that Jackson is picked
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testJacksonXmlString() throws Exception {
        WebTarget target = client.target(generateURL("/jxml/products/333"));
        Response response = target.request().get();
        String entity = response.readEntity(String.class);
        logger.info(entity);
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("The response entity content doesn't match the expected",
                "{\"name\":\"Iphone\",\"id\":333}", entity);
        response.close();

        target = client.target(generateURL("/jxml/products"));
        response = target.request().get();
        entity = response.readEntity(String.class);
        logger.info(entity);
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("The response entity content doesn't match the expected",
                "[{\"name\":\"Iphone\",\"id\":333},{\"name\":\"macbook\",\"id\":44}]", entity);
        response.close();
    }

    /**
     * @tpTestDetails Test that Jackson is picked and object can be returned from the resource
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testJackson() throws Exception {
        WebTarget target = client.target(generateURL("/products/333"));
        Response response = target.request().get();
        JacksonJaxbCoexistenceProduct2 p = response.readEntity(JacksonJaxbCoexistenceProduct2.class);
        Assert.assertEquals("JacksonJaxbCoexistenceProduct id value doesn't match", 333, p.getId());
        Assert.assertEquals("JacksonJaxbCoexistenceProduct name value doesn't match", "Iphone", p.getName());
        response.close();

        target = client.target(generateURL("/products"));
        response = target.request().get();
        String entity = response.readEntity(String.class);
        logger.info(entity);
        Assert.assertEquals(Status.OK, response.getStatus());
        response.close();

        target = client.target(generateURL("/products/333"));
        response = target.request().post(Entity.entity(p, "application/foo+json"));
        p = response.readEntity(JacksonJaxbCoexistenceProduct2.class);
        Assert.assertEquals("JacksonJaxbCoexistenceProduct id value doesn't match", 333, p.getId());
        Assert.assertEquals("JacksonJaxbCoexistenceProduct name value doesn't match", "Iphone", p.getName());
    }
}
