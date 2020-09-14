package io.quarkus.rest.test.providers.jaxb;

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
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.jaxb.resource.GenericResourceAbstractResource;
import io.quarkus.rest.test.providers.jaxb.resource.GenericResourceModel;
import io.quarkus.rest.test.providers.jaxb.resource.GenericResourceOtherAbstractResource;
import io.quarkus.rest.test.providers.jaxb.resource.GenericResourceResource;
import io.quarkus.rest.test.providers.jaxb.resource.GenericResourceResource2;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1125. Jaxb message body reader not recognized when using generics in
 *                    complex inheritance structure
 * @tpSince RESTEasy 3.0.16
 */
public class GenericResourceTest {

    String str = "<genericResourceModel></genericResourceModel>";

    static QuarkusRestClient client;
    protected static final Logger logger = Logger.getLogger(KeepCharsetTest.class.getName());

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, GenericResourceResource.class,
                            GenericResourceResource2.class,
                            GenericResourceModel.class, GenericResourceOtherAbstractResource.class,
                            GenericResourceAbstractResource.class);
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
        return PortProviderUtil.generateURL(path, GenericResourceTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Tests Jaxb object with resource using inheritance, generics and abstract classes
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testGenericInheritingResource() throws Exception {
        WebTarget target = client.target(generateURL("/test"));
        Response response = target.request().post(Entity.entity(str, "application/xml"));
        logger.info("status: " + response.getStatus());
        Assert.assertEquals(Status.OK, response.getStatus());
        String answer = response.readEntity(String.class);
        Assert.assertEquals("The response from the server is not the expected one", "Success!", answer);
        logger.info(answer);
    }

    /**
     * @tpTestDetails Tests Jaxb object with resource using inheritance, generics and abstract classes
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testGenericResource() throws Exception {
        WebTarget target = client.target(generateURL("/test2"));
        Response response = target.request().post(Entity.entity(str, "application/xml"));
        logger.info("status: " + response.getStatus());
        Assert.assertEquals(Status.OK, response.getStatus());
        String answer = response.readEntity(String.class);
        Assert.assertEquals("The response from the server is not the expected one", "Success!", answer);
        logger.info(answer);
    }
}
