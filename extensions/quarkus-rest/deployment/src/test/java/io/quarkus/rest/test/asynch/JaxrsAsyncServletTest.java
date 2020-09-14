package io.quarkus.rest.test.asynch;

import java.util.function.Supplier;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.plugins.providers.jaxb.JAXBXmlRootElementProvider;
import org.jboss.resteasy.utils.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.asynch.resource.JaxrsAsyncServletApp;
import io.quarkus.rest.test.asynch.resource.JaxrsAsyncServletAsyncResponseBlockingQueue;
import io.quarkus.rest.test.asynch.resource.JaxrsAsyncServletJaxrsResource;
import io.quarkus.rest.test.asynch.resource.JaxrsAsyncServletPrintingErrorHandler;
import io.quarkus.rest.test.asynch.resource.JaxrsAsyncServletResource;
import io.quarkus.rest.test.asynch.resource.JaxrsAsyncServletServiceUnavailableExceptionMapper;
import io.quarkus.rest.test.asynch.resource.JaxrsAsyncServletTimeoutHandler;
import io.quarkus.rest.test.asynch.resource.JaxrsAsyncServletXmlData;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Asynchronous RESTEasy
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for asyncHttpServlet module. Check cooperation during more requests and exception mapping.
 * @tpSince RESTEasy 3.0.16
 */
public class JaxrsAsyncServletTest {

    static QuarkusRestClient client;

    @Before
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(JaxrsAsyncServletXmlData.class, JaxrsAsyncServletAsyncResponseBlockingQueue.class,
                            JaxrsAsyncServletJaxrsResource.class,
                            JaxrsAsyncServletApp.class,
                            JaxrsAsyncServletTimeoutHandler.class, JaxrsAsyncServletResource.class,
                            JaxrsAsyncServletPrintingErrorHandler.class,
                            JaxrsAsyncServletServiceUnavailableExceptionMapper.class, JaxrsAsyncServletXmlData.class);
                    war.addAsWebInfResource(AsyncPostProcessingTest.class.getPackage(), "JaxrsAsyncServletWeb.xml", "web.xml");
                    return war;
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, AsyncServletTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Check ForbiddenException throwing during async request. Try to inject un-exist bean.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testInjectionFailure() throws Exception {
        long start = System.currentTimeMillis();
        Client client = ClientBuilder.newClient();
        Response response = client.target(generateURL("/jaxrs/injection-failure/abcd")).request().get();
        Assert.assertEquals(Response.Status.NOT_ACCEPTABLE.getStatusCode(), response.getStatus());
        Assert.assertTrue("ForbiddenException was not thrown",
                response.readEntity(String.class).contains(NotFoundException.class.getName()));
        long end = System.currentTimeMillis() - start;
        Assert.assertTrue("Wrong time of request", end < 1000); // should take less than 1 second
        response.close();
        client.close();
    }

    /**
     * @tpTestDetails Check ForbiddenException throwing during async request. No injection of some beans.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testMethodFailure() throws Exception {
        long start = System.currentTimeMillis();
        Client client = ClientBuilder.newClient();
        Response response = client.target(generateURL("/jaxrs/method-failure")).request().get();
        Assert.assertEquals(Response.Status.NOT_ACCEPTABLE.getStatusCode(), response.getStatus());
        Assert.assertTrue("ForbiddenException was not thrown",
                response.readEntity(String.class).contains(ForbiddenException.class.getName()));
        long end = System.currentTimeMillis() - start;
        Assert.assertTrue("Wrong time of request", end < 1000); // should take less than 1 second
        response.close();
        client.close();
    }

    /**
     * @tpTestDetails Try to get xml response.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testAsync() throws Exception {
        Client client = ClientBuilder.newClient();
        long start = System.currentTimeMillis();
        Response response = client.target(generateURL("/jaxrs")).request().get();
        long end = System.currentTimeMillis() - start;
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("Wrong content of response", "hello", response.readEntity(String.class));
        // The time out is set to 2 seconds, this is a best guess test and if future failures are present this should be
        // reconsidered with some sort of offset.
        Assert.assertTrue("Wrong time of request", end < 2000);
        response.close();
        client.close();
    }

    /**
     * @tpTestDetails Check timeout exception
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testTimeout() throws Exception {
        Client client = ClientBuilder.newClient();
        Response response = client.target(generateURL("/jaxrs/timeout")).request().get();
        Assert.assertEquals(Status.REQUEST_TIMEOUT, response.getStatus()); // exception mapper from another test overrides 503 to 408
        response.close();
        client.close();
    }

    /**
     * @tpTestDetails Test cooperation between two requests. Use 408 HTTP status.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testCancel() throws Exception {
        Client client = ClientBuilder.newClient();
        Response response = client.target(generateURL("/jaxrs/cancel")).request().get();
        Assert.assertEquals(Status.SERVICE_UNAVAILABLE, response.getStatus());
        response.close();

        // It is possible, that thread created in JaxrsAsyncServletJaxrsResource.cancel method
        // don't finish before next request is called. We need to wait some time and do this request again.
        // Default timeout is 20s
        boolean ok = false;
        for (int i = 0; i < TimeoutUtil.adjust(20); i++) {
            response = client.target(generateURL("/jaxrs/cancelled")).request().get();
            int status = response.getStatus();
            response.close();
            if (status == Status.NO_CONTENT) {
                ok = true;
                break;
            }
            Thread.sleep(1000);
        }
        Assert.assertTrue("Response was not canceled correctly", ok);
        client.close();
    }

    /**
     * @tpTestDetails Test cooperation between two requests. Use 200 HTTP status.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testResumeObject() throws Exception {
        Client client = ClientBuilder.newClient();
        client.register(JAXBXmlRootElementProvider.class);
        long start = System.currentTimeMillis();
        Response response = client.target(generateURL("/jaxrs/resume/object")).request().get();
        long end = System.currentTimeMillis() - start;
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("Wrong content of response", "bill", response.readEntity(JaxrsAsyncServletXmlData.class).getName());
        Assert.assertTrue("Wrong time of request", end < 1500);
        response.close();
        client.close();
    }

    /**
     * @tpTestDetails Create response in new thread.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testResumeObjectThread() throws Exception {
        Client client = ClientBuilder.newClient();
        client.register(JAXBXmlRootElementProvider.class);
        long start = System.currentTimeMillis();
        Response response = client.target(generateURL("/jaxrs/resume/object/thread")).request().get();
        long end = System.currentTimeMillis() - start;
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("Wrong content of response", "bill", response.readEntity(JaxrsAsyncServletXmlData.class).getName());
        Assert.assertTrue("Wrong time of request", end < 1000); // should take less than 1 second
        response.close();
        client.close();
    }
}
