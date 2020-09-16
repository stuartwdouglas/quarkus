package io.quarkus.rest.test.providers.jaxb;

import java.util.List;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.providers.jaxb.resource.JaxbCollectionFoo;
import io.quarkus.rest.test.providers.jaxb.resource.JaxbCollectionNamespacedFoo;
import io.quarkus.rest.test.providers.jaxb.resource.JaxbCollectionNamespacedResource;
import io.quarkus.rest.test.providers.jaxb.resource.JaxbCollectionResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Jaxb Collection Test")
public class JaxbCollectionTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(JaxbCollectionTest.class);
            return TestUtil.finishContainerPrepare(war, null, JaxbCollectionResource.class,
                    JaxbCollectionNamespacedResource.class, JaxbCollectionFoo.class, JaxbCollectionNamespacedFoo.class);
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
        return PortProviderUtil.generateURL(path, JaxbCollectionTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Client sends POST request with xml entity, the request is processed by resource, which can process
     *                JAXB objects wrapped in collection element.
     * @tpPassCrit The Response contains correct number of elements and correct values
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Naked Array")
    public void testNakedArray() throws Exception {
        String xml = "<resteasy:collection xmlns:resteasy=\"http://jboss.org/resteasy\">"
                + "<foo test=\"hello\"/></resteasy:collection>";
        QuarkusRestWebTarget target = client.target(generateURL("/array"));
        Response response = target.request().accept("application/xml").post(Entity.xml(xml));
        List<JaxbCollectionFoo> list = response.readEntity(new javax.ws.rs.core.GenericType<List<JaxbCollectionFoo>>() {
        });
        Assertions.assertEquals(1, list.size(), "The response doesn't contain 1 item, which is expected");
        Assertions.assertEquals(list.get(0).getTest(), "hello", "The response doesn't contain correct element value");
        response.close();
    }

    /**
     * @tpTestDetails Client sends POST request with xml entity, the request is processed by resource, which can process
     *                JAXB objects wrapped in collection element. The resource has changed the collection element name
     *                using @Wrapped
     *                annotation on the resource to 'list'.
     * @tpPassCrit The Response contains correct number of elements and correct values
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test List")
    public void testList() throws Exception {
        String xml = "<list>" + "<foo test=\"hello\"/></list>";
        QuarkusRestWebTarget target = client.target(generateURL("/list"));
        Response response = target.request().post(Entity.xml(xml));
        JaxbCollectionFoo[] list = response.readEntity(new javax.ws.rs.core.GenericType<JaxbCollectionFoo[]>() {
        });
        Assertions.assertEquals(1, list.length, "The response doesn't contain 1 item, which is expected");
        Assertions.assertEquals(list[0].getTest(), "hello", "The response doesn't contain correct element value");
        response.close();
    }

    /**
     * @tpTestDetails Client sends POST request with xml entity, the request is processed by resource, which can process
     *                JAXB objects wrapped in collection element. The XML element of name 'foo' has changed namespace to
     *                'http://foo.com'.
     * @tpPassCrit The Response contains correct number of elements and correct values
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Namespaced Naked Array")
    public void testNamespacedNakedArray() throws Exception {
        String xml = "<collection xmlns:foo=\"http://foo.com\">" + "<foo:foo test=\"hello\"/></collection>";
        QuarkusRestWebTarget target = client.target(generateURL("/namespaced/array"));
        Response response = target.request().post(Entity.xml(xml));
        List<JaxbCollectionNamespacedFoo> list = response
                .readEntity(new javax.ws.rs.core.GenericType<List<JaxbCollectionNamespacedFoo>>() {
                });
        Assertions.assertEquals(1, list.size(), "The response doesn't contain 1 item, which is expected");
        Assertions.assertEquals(list.get(0).getTest(), "hello", "The response doesn't contain correct element value");
        response.close();
    }

    /**
     * @tpTestDetails Client sends POST request with xml entity, the request is processed by resource, which can process
     *                JAXB objects wrapped in collection element. The resource has changed the collection element name
     *                using @Wrapped
     *                annotation on the resource to 'list'. The XML element of name 'foo' has changed namespace to
     *                'http://foo.com'.
     * @tpPassCrit The Response contains correct number of elements and correct values
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Namespaced List")
    public void testNamespacedList() throws Exception {
        String xml = "<list xmlns:foo=\"http://foo.com\">" + "<foo:foo test=\"hello\"/></list>";
        QuarkusRestWebTarget target = client.target(generateURL("/namespaced/list"));
        Response response = target.request().post(Entity.xml(xml));
        JaxbCollectionNamespacedFoo[] list = response
                .readEntity(new javax.ws.rs.core.GenericType<JaxbCollectionNamespacedFoo[]>() {
                });
        Assertions.assertEquals(1, list.length, "The response doesn't contain 1 item, which is expected");
        Assertions.assertEquals(list[0].getTest(), "hello", "The response doesn't contain correct element value");
        response.close();
    }

    /**
     * @tpTestDetails Client sends POST request with xml entity containing wrong element name for collection.
     * @tpPassCrit Response with code BAD REQUEST
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Bad List")
    public void testBadList() throws Exception {
        String xml = "<bad-list>" + "<foo test=\"hello\"/></bad-list>";
        QuarkusRestWebTarget target = client.target(generateURL("/list"));
        Response response = target.request().post(Entity.xml(xml));
        Assertions.assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        response.close();
    }
}
