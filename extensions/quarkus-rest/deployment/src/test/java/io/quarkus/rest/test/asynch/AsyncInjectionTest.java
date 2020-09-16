package io.quarkus.rest.test.asynch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.asynch.resource.AsyncInjectionBooleanInjector;
import io.quarkus.rest.test.asynch.resource.AsyncInjectionByteInjector;
import io.quarkus.rest.test.asynch.resource.AsyncInjectionCharInjector;
import io.quarkus.rest.test.asynch.resource.AsyncInjectionContext;
import io.quarkus.rest.test.asynch.resource.AsyncInjectionContextAsyncSpecifier;
import io.quarkus.rest.test.asynch.resource.AsyncInjectionContextErrorSpecifier;
import io.quarkus.rest.test.asynch.resource.AsyncInjectionContextInjector;
import io.quarkus.rest.test.asynch.resource.AsyncInjectionContextInterface;
import io.quarkus.rest.test.asynch.resource.AsyncInjectionContextInterfaceInjector;
import io.quarkus.rest.test.asynch.resource.AsyncInjectionDoubleInjector;
import io.quarkus.rest.test.asynch.resource.AsyncInjectionException;
import io.quarkus.rest.test.asynch.resource.AsyncInjectionExceptionMapper;
import io.quarkus.rest.test.asynch.resource.AsyncInjectionFloatInjector;
import io.quarkus.rest.test.asynch.resource.AsyncInjectionIntInjector;
import io.quarkus.rest.test.asynch.resource.AsyncInjectionLongInjector;
import io.quarkus.rest.test.asynch.resource.AsyncInjectionPrimitiveInjectorSpecifier;
import io.quarkus.rest.test.asynch.resource.AsyncInjectionResource;
import io.quarkus.rest.test.asynch.resource.AsyncInjectionResource2;
import io.quarkus.rest.test.asynch.resource.AsyncInjectionShortInjector;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails Async Request Filter test.
 * @tpSince RESTEasy 4.0.0
 */
// @RunWith(UndertowTestRunner.class)
@DisplayName("Async Injection Test")
public class AsyncInjectionTest {

    protected static final Logger log = Logger.getLogger(AsyncInjectionTest.class.getName());

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, AsyncInjectionResource.class, AsyncInjectionContext.class,
                    AsyncInjectionContextInjector.class, AsyncInjectionContextInterface.class,
                    AsyncInjectionContextInterfaceInjector.class, AsyncInjectionContextAsyncSpecifier.class,
                    AsyncInjectionContextErrorSpecifier.class, AsyncInjectionException.class,
                    AsyncInjectionExceptionMapper.class, AsyncInjectionResource2.class,
                    AsyncInjectionPrimitiveInjectorSpecifier.class, AsyncInjectionBooleanInjector.class,
                    AsyncInjectionCharInjector.class, AsyncInjectionByteInjector.class, AsyncInjectionShortInjector.class,
                    AsyncInjectionIntInjector.class, AsyncInjectionLongInjector.class, AsyncInjectionFloatInjector.class,
                    AsyncInjectionDoubleInjector.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, AsyncInjectionTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Async Injection works
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    @DisplayName("Test Async Injection")
    public void testAsyncInjection() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget base = client.target(generateURL("/"));
        Response response = base.request().get();
        assertEquals("Non-200 result: " + response.readEntity(String.class), 200, response.getStatus());
        client.close();
    }

    /**
     * @tpTestDetails Async Injection works for interfaces
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    @DisplayName("Test Async Injection Interface")
    public void testAsyncInjectionInterface() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget base = client.target(generateURL("/interface"));
        Response response = base.request().get();
        assertEquals("Non-200 result: " + response.readEntity(String.class), 200, response.getStatus());
        client.close();
    }

    /**
     * @tpTestDetails Async Injection does not suspend request if already resolved
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    @DisplayName("Test Async Injection Resolved")
    public void testAsyncInjectionResolved() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget base = client.target(generateURL("/resolved"));
        Response response = base.request().get();
        assertEquals("Non-200 result: " + response.readEntity(String.class), 200, response.getStatus());
        client.close();
    }

    /**
     * @tpTestDetails Async Injection suspends request if not yet resolved
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    @DisplayName("Test Async Injection Suspended")
    public void testAsyncInjectionSuspended() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget base = client.target(generateURL("/suspended"));
        Response response = base.request().get();
        assertEquals("Non-200 result: " + response.readEntity(String.class), 200, response.getStatus());
        client.close();
    }

    /**
     * @tpTestDetails Async Injection with exceptions
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    @DisplayName("Test Async Injection Exception")
    public void testAsyncInjectionException() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget base = client.target(generateURL("/exception"));
        Response response = base.request().get();
        assertEquals("Non-202 result: " + response.readEntity(String.class), 202, response.getStatus());
        client.close();
    }

    /**
     * @tpTestDetails Async Injection with async exceptions
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    @DisplayName("Test Async Injection Exception Async")
    public void testAsyncInjectionExceptionAsync() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget base = client.target(generateURL("/exception-async"));
        Response response = base.request().get();
        assertEquals("Non-202 result: " + response.readEntity(String.class), 202, response.getStatus());
        client.close();
    }

    /**
     * @tpTestDetails Async Injection in places where it does not work
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    @DisplayName("Test Async Injection Exception Late")
    public void testAsyncInjectionExceptionLate() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget base = client.target(generateURL("/late"));
        Response response = base.request().get();
        assertEquals("Non-200 result: " + response.readEntity(String.class), 200, response.getStatus());
        client.close();
    }

    /**
     * @tpTestDetails Async Injection of primitive types
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    @DisplayName("Test Async Injection Primitives")
    public void testAsyncInjectionPrimitives() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget base = client.target(generateURL("/primitives"));
        Response response = base.request().get();
        assertEquals("Non-200 result: " + response.readEntity(String.class), 200, response.getStatus());
        client.close();
    }
}
