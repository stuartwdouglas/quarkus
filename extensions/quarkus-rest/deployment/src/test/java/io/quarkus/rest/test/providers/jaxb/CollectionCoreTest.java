package io.quarkus.rest.test.providers.jaxb;

import static org.hamcrest.CoreMatchers.containsString;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
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
import io.quarkus.rest.test.providers.jaxb.resource.CollectionCustomer;
import io.quarkus.rest.test.providers.jaxb.resource.CollectionNamespacedCustomer;
import io.quarkus.rest.test.providers.jaxb.resource.CollectionNamespacedResource;
import io.quarkus.rest.test.providers.jaxb.resource.CollectionResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Check jaxb requests with collection
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Collection Core Test")
public class CollectionCoreTest {

    private static final String WRONG_RESPONSE = "Response contains wrong data";

    protected static final Logger logger = Logger.getLogger(CollectionCoreTest.class.getName());

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(CollectionCustomer.class, CollectionNamespacedCustomer.class);
            return TestUtil.finishContainerPrepare(war, null, CollectionResource.class, CollectionNamespacedResource.class);
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
        return PortProviderUtil.generateURL(path, CollectionCoreTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test array response
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Array")
    public void testArray() throws Exception {
        Invocation.Builder request = client.target(generateURL("/array")).request();
        Response response = request.get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String str = response.readEntity(String.class);
        logger.info(String.format("Response: %s", str));
        response.close();
        response = request.put(Entity.entity(str, "application/xml"));
        Assertions.assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Test list response
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test List")
    public void testList() throws Exception {
        Invocation.Builder request = client.target(generateURL("/list")).request();
        Response response = request.get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String str = response.readEntity(String.class);
        logger.info(String.format("Response: %s", str));
        response.close();
        response = request.put(Entity.entity(str, "application/xml"));
        Assertions.assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Test GenericEntity of list response
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Response")
    public void testResponse() throws Exception {
        Invocation.Builder request = client.target(generateURL("/list/response")).request();
        Response response = request.get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        logger.info(String.format("Response: %s", response.readEntity(String.class)));
    }

    /**
     * @tpTestDetails Test array of customers with namespace in XML
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Namespaced Array")
    public void testNamespacedArray() throws Exception {
        Invocation.Builder request = client.target(generateURL("/namespaced/array")).request();
        Response response = request.get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String str = response.readEntity(String.class);
        logger.info(String.format("Response: %s", str));
        response.close();
        response = request.put(Entity.entity(str, "application/xml"));
        Assertions.assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        response.close();
        Assert.assertThat(WRONG_RESPONSE, str, containsString("http://customer.com"));
    }

    /**
     * @tpTestDetails Test GenericEntity with list of customers with namespace in XML
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Namespaced List")
    public void testNamespacedList() throws Exception {
        Invocation.Builder request = client.target(generateURL("/namespaced/list")).request();
        Response response = request.get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String str = response.readEntity(String.class);
        logger.info(String.format("Response: %s", str));
        response.close();
        response = request.put(Entity.entity(str, "application/xml"));
        Assertions.assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        response.close();
        Assert.assertThat(WRONG_RESPONSE, str, containsString("http://customer.com"));
    }

    /**
     * @tpTestDetails Test list of customers with namespace in XML
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Namespaced Response")
    public void testNamespacedResponse() throws Exception {
        Invocation.Builder request = client.target(generateURL("/namespaced/list/response")).request();
        Response response = request.get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String str = response.readEntity(String.class);
        logger.info(String.format("Response: %s", str));
        response.close();
        Assert.assertThat(WRONG_RESPONSE, str, containsString("http://customer.com"));
    }
}
