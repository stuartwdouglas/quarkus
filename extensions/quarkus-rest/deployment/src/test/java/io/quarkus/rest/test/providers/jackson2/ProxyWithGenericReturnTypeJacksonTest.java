package io.quarkus.rest.test.providers.jackson2;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
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
import io.quarkus.rest.test.providers.jackson2.resource.ProxyWithGenericReturnTypeJacksonAbstractParent;
import io.quarkus.rest.test.providers.jackson2.resource.ProxyWithGenericReturnTypeJacksonResource;
import io.quarkus.rest.test.providers.jackson2.resource.ProxyWithGenericReturnTypeJacksonSubResourceIntf;
import io.quarkus.rest.test.providers.jackson2.resource.ProxyWithGenericReturnTypeJacksonSubResourceSubIntf;
import io.quarkus.rest.test.providers.jackson2.resource.ProxyWithGenericReturnTypeJacksonType1;
import io.quarkus.rest.test.providers.jackson2.resource.ProxyWithGenericReturnTypeJacksonType2;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jackson2 provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class ProxyWithGenericReturnTypeJacksonTest {

    protected static final Logger logger = Logger.getLogger(ProxyWithGenericReturnTypeJacksonTest.class.getName());
    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(Jackson2Test.class);
                    return TestUtil.finishContainerPrepare(war, null, ProxyWithGenericReturnTypeJacksonAbstractParent.class,
                            ProxyWithGenericReturnTypeJacksonResource.class,
                            ProxyWithGenericReturnTypeJacksonSubResourceIntf.class,
                            ProxyWithGenericReturnTypeJacksonSubResourceSubIntf.class,
                            ProxyWithGenericReturnTypeJacksonType1.class,
                            ProxyWithGenericReturnTypeJacksonType2.class);
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
        return PortProviderUtil.generateURL(path, ProxyWithGenericReturnTypeJacksonTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Tests usage of proxied subresource
     * @tpPassCrit The resource returns Success response
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testProxyWithGenericReturnType() throws Exception {
        WebTarget target = client.target(generateURL("/test/one/"));
        logger.info("Sending request");
        Response response = target.request().get();
        String entity = response.readEntity(String.class);
        logger.info("Received response: " + entity);
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertTrue("Type property is missing.", entity.contains("type"));
        response.close();

        target = client.target(generateURL("/test/list/"));
        logger.info("Sending request");
        response = target.request().get();
        entity = response.readEntity(String.class);
        logger.info("Received response: " + entity);
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertTrue("Type property is missing.", entity.contains("type"));
        response.close();
    }
}
