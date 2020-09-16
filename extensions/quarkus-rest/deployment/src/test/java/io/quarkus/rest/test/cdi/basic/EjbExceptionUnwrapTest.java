package io.quarkus.rest.test.cdi.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.cdi.basic.resource.EjbExceptionUnwrapFooException;
import io.quarkus.rest.test.cdi.basic.resource.EjbExceptionUnwrapFooExceptionMapper;
import io.quarkus.rest.test.cdi.basic.resource.EjbExceptionUnwrapFooResource;
import io.quarkus.rest.test.cdi.basic.resource.EjbExceptionUnwrapFooResourceBean;
import io.quarkus.rest.test.cdi.basic.resource.EjbExceptionUnwrapLocatingResource;
import io.quarkus.rest.test.cdi.basic.resource.EjbExceptionUnwrapLocatingResourceBean;
import io.quarkus.rest.test.cdi.basic.resource.EjbExceptionUnwrapSimpleResource;
import io.quarkus.rest.test.cdi.basic.resource.EjbExceptionUnwrapSimpleResourceBean;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for unwrapping EJB exception
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Ejb Exception Unwrap Test")
public class EjbExceptionUnwrapTest {

    static Client client;

    @BeforeAll
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void close() {
        client.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(EjbExceptionUnwrapFooException.class, EjbExceptionUnwrapFooResource.class,
                    EjbExceptionUnwrapLocatingResource.class, EjbExceptionUnwrapSimpleResource.class);
            return TestUtil.finishContainerPrepare(war, null, EjbExceptionUnwrapFooExceptionMapper.class,
                    EjbExceptionUnwrapSimpleResourceBean.class, EjbExceptionUnwrapLocatingResourceBean.class,
                    EjbExceptionUnwrapFooResourceBean.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, EjbExceptionUnwrapTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails No default resource for exception
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test No Defaults Resource For Exception")
    public void testNoDefaultsResourceForException() {
        Response response = client.target(generateURL("/exception")).request().get();
        Assertions.assertEquals(Status.CONFLICT.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails No default resource without exception mapping
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test No Defaults Resource")
    public void testNoDefaultsResource() throws Exception {
        {
            Response response = client.target(generateURL("/basic")).request().get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            response.close();
        }
        {
            Response response = client.target(generateURL("/basic")).request().put(Entity.entity("basic", "text/plain"));
            Assertions.assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
            response.close();
        }
        {
            Response response = client.target(generateURL("/queryParam")).queryParam("param", "hello world").request().get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals(response.readEntity(String.class), "hello world");
            response.close();
        }
        {
            Response response = client.target(generateURL("/uriParam/1234")).request().get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals(response.readEntity(String.class), "1234");
            response.close();
        }
    }

    /**
     * @tpTestDetails Check for locating resource
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Locating Resource")
    public void testLocatingResource() throws Exception {
        {
            Response response = client.target(generateURL("/locating/basic")).request().get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals(response.readEntity(String.class), "basic");
            response.close();
        }
        {
            Response response = client.target(generateURL("/locating/basic")).request()
                    .put(Entity.entity("basic", "text/plain"));
            Assertions.assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
            response.close();
        }
        {
            Response response = client.target(generateURL("/locating/queryParam")).queryParam("param", "hello world").request()
                    .get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals(response.readEntity(String.class), "hello world");
            response.close();
        }
        {
            Response response = client.target(generateURL("/locating/uriParam/1234")).request().get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals(response.readEntity(String.class), "1234");
            response.close();
        }
    }
}
