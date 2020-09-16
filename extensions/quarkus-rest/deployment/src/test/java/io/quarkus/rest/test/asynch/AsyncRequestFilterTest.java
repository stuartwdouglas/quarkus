package io.quarkus.rest.test.asynch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.asynch.resource.AsyncFilterException;
import io.quarkus.rest.test.asynch.resource.AsyncFilterExceptionMapper;
import io.quarkus.rest.test.asynch.resource.AsyncPreMatchRequestFilter1;
import io.quarkus.rest.test.asynch.resource.AsyncPreMatchRequestFilter2;
import io.quarkus.rest.test.asynch.resource.AsyncPreMatchRequestFilter3;
import io.quarkus.rest.test.asynch.resource.AsyncRequestFilter;
import io.quarkus.rest.test.asynch.resource.AsyncRequestFilter1;
import io.quarkus.rest.test.asynch.resource.AsyncRequestFilter2;
import io.quarkus.rest.test.asynch.resource.AsyncRequestFilter3;
import io.quarkus.rest.test.asynch.resource.AsyncRequestFilterResource;
import io.quarkus.rest.test.asynch.resource.AsyncResponseFilter;
import io.quarkus.rest.test.asynch.resource.AsyncResponseFilter1;
import io.quarkus.rest.test.asynch.resource.AsyncResponseFilter2;
import io.quarkus.rest.test.asynch.resource.AsyncResponseFilter3;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails Async Request Filter test.
 * @tpSince RESTEasy 4.0.0
 */
@DisplayName("Async Request Filter Test")
public class AsyncRequestFilterTest {

    protected static final Logger log = Logger.getLogger(AsyncRequestFilterTest.class.getName());

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(AsyncRequestFilterResource.class, AsyncRequestFilter.class, AsyncResponseFilter.class,
                    AsyncRequestFilter1.class, AsyncRequestFilter2.class, AsyncRequestFilter3.class,
                    AsyncPreMatchRequestFilter1.class, AsyncPreMatchRequestFilter2.class, AsyncPreMatchRequestFilter3.class,
                    AsyncResponseFilter1.class, AsyncResponseFilter2.class, AsyncResponseFilter3.class,
                    AsyncFilterException.class, AsyncFilterExceptionMapper.class);
            return war;
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, AsyncRequestFilterTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Interceptors work
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    @DisplayName("Test Request Filters")
    public void testRequestFilters() throws Exception {
        Client client = ClientBuilder.newClient();
        // Create book.
        WebTarget base = client.target(generateURL("/"));
        // all sync
        Response response = base.request().header("Filter1", "sync-pass").header("Filter2", "sync-pass")
                .header("Filter3", "sync-pass").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "resource");
        response = base.request().header("Filter1", "sync-fail").header("Filter2", "sync-fail").header("Filter3", "sync-fail")
                .get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "Filter1");
        response = base.request().header("Filter1", "sync-pass").header("Filter2", "sync-fail").header("Filter3", "sync-fail")
                .get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "Filter2");
        response = base.request().header("Filter1", "sync-pass").header("Filter2", "sync-pass").header("Filter3", "sync-fail")
                .get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "Filter3");
        // async
        response = base.request().header("Filter1", "async-pass").header("Filter2", "sync-pass").header("Filter3", "sync-pass")
                .get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "resource");
        response = base.request().header("Filter1", "async-pass").header("Filter2", "async-pass").header("Filter3", "sync-pass")
                .get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "resource");
        response = base.request().header("Filter1", "async-pass").header("Filter2", "async-pass")
                .header("Filter3", "async-pass").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "resource");
        response = base.request().header("Filter1", "async-pass").header("Filter2", "sync-pass").header("Filter3", "async-pass")
                .get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "resource");
        response = base.request().header("Filter1", "sync-pass").header("Filter2", "async-pass").header("Filter3", "sync-pass")
                .get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "resource");
        // async failures
        response = base.request().header("Filter1", "async-fail").header("Filter2", "sync-fail").header("Filter3", "sync-fail")
                .get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "Filter1");
        response = base.request().header("Filter1", "async-pass").header("Filter2", "sync-fail").header("Filter3", "sync-pass")
                .get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "Filter2");
        response = base.request().header("Filter1", "async-pass").header("Filter2", "async-fail").header("Filter3", "sync-pass")
                .get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "Filter2");
        // async instantaneous
        response = base.request().header("Filter1", "async-pass-instant").header("Filter2", "sync-pass")
                .header("Filter3", "sync-pass").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "resource");
        response = base.request().header("Filter1", "async-fail-instant").header("Filter2", "sync-pass")
                .header("Filter3", "sync-pass").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "Filter1");
        client.close();
    }

    /**
     * @tpTestDetails Interceptors work
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    @DisplayName("Test Pre Match Request Filters")
    public void testPreMatchRequestFilters() throws Exception {
        Client client = ClientBuilder.newClient();
        // Create book.
        WebTarget base = client.target(generateURL("/"));
        // all sync
        Response response = base.request().header("PreMatchFilter1", "sync-pass").header("PreMatchFilter2", "sync-pass")
                .header("PreMatchFilter3", "sync-pass").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "resource");
        response = base.request().header("PreMatchFilter1", "sync-fail").header("PreMatchFilter2", "sync-fail")
                .header("PreMatchFilter3", "sync-fail").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "PreMatchFilter1");
        response = base.request().header("PreMatchFilter1", "sync-pass").header("PreMatchFilter2", "sync-fail")
                .header("PreMatchFilter3", "sync-fail").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "PreMatchFilter2");
        response = base.request().header("PreMatchFilter1", "sync-pass").header("PreMatchFilter2", "sync-pass")
                .header("PreMatchFilter3", "sync-fail").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "PreMatchFilter3");
        // async
        response = base.request().header("PreMatchFilter1", "async-pass").header("PreMatchFilter2", "sync-pass")
                .header("PreMatchFilter3", "sync-pass").get();
        assertEquals(response.readEntity(String.class), "resource");
        assertEquals(200, response.getStatus());
        response = base.request().header("PreMatchFilter1", "async-pass").header("PreMatchFilter2", "async-pass")
                .header("PreMatchFilter3", "sync-pass").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "resource");
        response = base.request().header("PreMatchFilter1", "async-pass").header("PreMatchFilter2", "async-pass")
                .header("PreMatchFilter3", "async-pass").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "resource");
        response = base.request().header("PreMatchFilter1", "async-pass").header("PreMatchFilter2", "sync-pass")
                .header("PreMatchFilter3", "async-pass").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "resource");
        response = base.request().header("PreMatchFilter1", "sync-pass").header("PreMatchFilter2", "async-pass")
                .header("PreMatchFilter3", "sync-pass").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "resource");
        // async failures
        response = base.request().header("PreMatchFilter1", "async-fail").header("PreMatchFilter2", "sync-fail")
                .header("PreMatchFilter3", "sync-fail").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "PreMatchFilter1");
        response = base.request().header("PreMatchFilter1", "async-pass").header("PreMatchFilter2", "sync-fail")
                .header("PreMatchFilter3", "sync-pass").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "PreMatchFilter2");
        response = base.request().header("PreMatchFilter1", "async-pass").header("PreMatchFilter2", "async-fail")
                .header("PreMatchFilter3", "sync-pass").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "PreMatchFilter2");
        client.close();
    }

    /**
     * @tpTestDetails Interceptors work
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    @DisplayName("Test Response Filters")
    public void testResponseFilters() throws Exception {
        Client client = ClientBuilder.newClient();
        // Create book.
        WebTarget base = client.target(generateURL("/"));
        // all sync
        Response response = base.request().header("ResponseFilter1", "sync-pass").header("ResponseFilter2", "sync-pass")
                .header("ResponseFilter3", "sync-pass").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "resource");
        response = base.request().header("ResponseFilter1", "sync-fail").header("ResponseFilter2", "sync-pass")
                .header("ResponseFilter3", "sync-pass").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "ResponseFilter1");
        response = base.request().header("ResponseFilter1", "sync-pass").header("ResponseFilter2", "sync-fail")
                .header("ResponseFilter3", "sync-pass").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "ResponseFilter2");
        response = base.request().header("ResponseFilter1", "sync-pass").header("ResponseFilter2", "sync-pass")
                .header("ResponseFilter3", "sync-fail").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "ResponseFilter3");
        // async
        response = base.request().header("ResponseFilter1", "async-pass").header("ResponseFilter2", "sync-pass")
                .header("ResponseFilter3", "sync-pass").get();
        assertEquals(response.readEntity(String.class), "resource");
        assertEquals(200, response.getStatus());
        response = base.request().header("ResponseFilter1", "async-pass").header("ResponseFilter2", "async-pass")
                .header("ResponseFilter3", "sync-pass").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "resource");
        response = base.request().header("ResponseFilter1", "async-pass").header("ResponseFilter2", "async-pass")
                .header("ResponseFilter3", "async-pass").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "resource");
        response = base.request().header("ResponseFilter1", "async-pass").header("ResponseFilter2", "sync-pass")
                .header("ResponseFilter3", "async-pass").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "resource");
        response = base.request().header("ResponseFilter1", "sync-pass").header("ResponseFilter2", "async-pass")
                .header("ResponseFilter3", "sync-pass").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "resource");
        // async failures
        response = base.request().header("ResponseFilter1", "async-fail").header("ResponseFilter2", "sync-pass")
                .header("ResponseFilter3", "sync-pass").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "ResponseFilter1");
        response = base.request().header("ResponseFilter1", "async-pass").header("ResponseFilter2", "sync-fail")
                .header("ResponseFilter3", "sync-pass").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "ResponseFilter2");
        response = base.request().header("ResponseFilter1", "async-pass").header("ResponseFilter2", "async-fail")
                .header("ResponseFilter3", "sync-pass").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "ResponseFilter2");
        // async instantaneous
        response = base.request().header("ResponseFilter1", "async-pass-instant").header("ResponseFilter2", "sync-pass")
                .header("ResponseFilter3", "sync-pass").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "resource");
        response = base.request().header("ResponseFilter1", "async-fail-instant").header("ResponseFilter2", "sync-pass")
                .header("ResponseFilter3", "sync-pass").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "ResponseFilter1");
        client.close();
    }

    /**
     * @tpTestDetails Interceptors work
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    @DisplayName("Test Response Filters 2")
    public void testResponseFilters2() throws Exception {
        Client client = ClientBuilder.newClient();
        // Create book.
        WebTarget base = client.target(generateURL("/async"));
        // async way later
        Response response = base.request().header("ResponseFilter1", "sync-pass").header("ResponseFilter2", "sync-pass")
                .header("ResponseFilter3", "async-fail-late").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "ResponseFilter3");
        client.close();
    }

    /**
     * @tpTestDetails Async filters work with resume(Throwable) wrt filters/callbacks/complete
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    @DisplayName("Test Response Filters Throw")
    public void testResponseFiltersThrow() throws Exception {
        Client client = ClientBuilder.newClient();
        testResponseFilterThrow(client, "/callback-async", false);
        testResponseFilterThrow(client, "/callback", false);
        testResponseFilterThrow(client, "/callback-async", true);
        testResponseFilterThrow(client, "/callback", true);
        client.close();
    }

    private void testResponseFilterThrow(Client client, String target, boolean useExceptionMapper) {
        WebTarget base = client.target(generateURL(target));
        // throw in response filter
        Response response = base.request().header("ResponseFilter1", "sync-pass").header("ResponseFilter2", "sync-pass")
                .header("UseExceptionMapper", useExceptionMapper).header("ResponseFilter3", "async-throw-late").get();
        // this is 500 even with exception mapper because exceptions in response filters are not mapped
        assertEquals(500, response.getStatus());
        try {
            // give a chance to CI to run the callbacks
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        // check that callbacks were called
        response = base.request().get();
        assertEquals(200, response.getStatus());
        if (useExceptionMapper)
            assertEquals(response.getHeaders().getFirst("ResponseFilterCallbackResponseFilter3"),
                    "io.quarkus.rest.test.asynch.resource.AsyncFilterException: ouch");
        else
            assertEquals(response.getHeaders().getFirst("ResponseFilterCallbackResponseFilter3"), "java.lang.Throwable: ouch");
        // throw in request filter
        response = base.request().header("Filter1", "sync-pass").header("Filter2", "sync-pass")
                .header("UseExceptionMapper", useExceptionMapper).header("Filter3", "async-throw-late").get();
        if (useExceptionMapper) {
            assertEquals(Status.ACCEPTED.getStatusCode(), response.getStatus());
            assertEquals(response.readEntity(String.class), "exception was mapped");
        } else {
            assertEquals(500, response.getStatus());
        }
        try {
            // give a chance to CI to run the callbacks
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        // check that callbacks were called
        response = base.request().get();
        assertEquals(200, response.getStatus());
        if (useExceptionMapper)
            assertEquals(response.getHeaders().getFirst("RequestFilterCallbackFilter3"),
                    "io.quarkus.rest.test.asynch.resource.AsyncFilterException: ouch");
        else
            assertEquals(response.getHeaders().getFirst("RequestFilterCallbackFilter3"), "java.lang.Throwable: ouch");
    }

    /**
     * @tpTestDetails Interceptors work with non-Response resource methods
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    @DisplayName("Test Request Filters Guess Return Type")
    public void testRequestFiltersGuessReturnType() throws Exception {
        Client client = ClientBuilder.newClient();
        // Create book.
        WebTarget base = client.target(generateURL("/non-response"));
        Response response = base.request().header("Filter1", "async-pass").header("Filter2", "sync-pass")
                .header("Filter3", "sync-pass").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "resource");
        client.close();
    }
}
