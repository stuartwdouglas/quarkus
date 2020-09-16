package io.quarkus.rest.test.providers.jackson2;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

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
@DisplayName("Proxy With Generic Return Type Jackson Test")
public class ProxyWithGenericReturnTypeJacksonTest {

    protected static final Logger logger = Logger.getLogger(ProxyWithGenericReturnTypeJacksonTest.class.getName());

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(Jackson2Test.class);
            return TestUtil.finishContainerPrepare(war, null, ProxyWithGenericReturnTypeJacksonAbstractParent.class,
                    ProxyWithGenericReturnTypeJacksonResource.class, ProxyWithGenericReturnTypeJacksonSubResourceIntf.class,
                    ProxyWithGenericReturnTypeJacksonSubResourceSubIntf.class, ProxyWithGenericReturnTypeJacksonType1.class,
                    ProxyWithGenericReturnTypeJacksonType2.class);
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
        return PortProviderUtil.generateURL(path, ProxyWithGenericReturnTypeJacksonTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Tests usage of proxied subresource
     * @tpPassCrit The resource returns Success response
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Proxy With Generic Return Type")
    public void testProxyWithGenericReturnType() throws Exception {
        WebTarget target = client.target(generateURL("/test/one/"));
        logger.info("Sending request");
        Response response = target.request().get();
        String entity = response.readEntity(String.class);
        logger.info("Received response: " + entity);
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertTrue(entity.contains("type"), "Type property is missing.");
        response.close();
        target = client.target(generateURL("/test/list/"));
        logger.info("Sending request");
        response = target.request().get();
        entity = response.readEntity(String.class);
        logger.info("Received response: " + entity);
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertTrue(entity.contains("type"), "Type property is missing.");
        response.close();
    }
}
