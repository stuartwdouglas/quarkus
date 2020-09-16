package io.quarkus.rest.test.client.exception;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.Supplier;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.rules.ExpectedException;

import io.quarkus.rest.test.client.ClientTestBase;
import io.quarkus.rest.test.client.exception.resource.ClientExceptionsCustomException;
import io.quarkus.rest.test.client.exception.resource.ClientExceptionsCustomExceptionRequestFilter;
import io.quarkus.rest.test.client.exception.resource.ClientExceptionsCustomExceptionResponseFilter;
import io.quarkus.rest.test.client.exception.resource.ClientExceptionsData;
import io.quarkus.rest.test.client.exception.resource.ClientExceptionsIOExceptionReaderInterceptor;
import io.quarkus.rest.test.client.exception.resource.ClientExceptionsIOExceptionRequestFilter;
import io.quarkus.rest.test.client.exception.resource.ClientExceptionsIOExceptionResponseFilter;
import io.quarkus.rest.test.client.exception.resource.ClientExceptionsResource;
import io.quarkus.rest.test.client.exception.resource.ClientExceptionsRuntimeExceptionRequestFilter;
import io.quarkus.rest.test.client.exception.resource.ClientExceptionsRuntimeExceptionResponseFilter;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @author <a href="mailto:kanovotn@redhat.com">Katerina Novotna</a>
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test if client throws exceptions as described in JAXRS-2.0 specification:
 *                    "When a provider method throws an exception, the JAX-RS client runtime will map it to an instance of
 *                    ProcessingException if thrown while processing a request, and to a ResponseProcessingException
 *                    if thrown while processing a response."
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Client Exceptions Test")
public class ClientExceptionsTest extends ClientTestBase {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(ClientExceptionsData.class);
            return TestUtil.finishContainerPrepare(war, null, ClientExceptionsResource.class);
        }
    });

    @BeforeEach
    public void before() {
        client = ClientBuilder.newClient();
    }

    @AfterEach
    public void close() {
        client.close();
    }

    /**
     * @tpTestDetails Send a request for entity which requires special MessageBodyReader which is not available.
     *                The exception is raised before sending request to the server
     * @tpPassCrit ProcessingException is raised
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("No Message Body Reader Exists Test")
    public void noMessageBodyReaderExistsTest() {
        WebTarget base = client.target(generateURL("/") + "senddata");
        thrown.expect(ProcessingException.class);
        base.request().post(Entity.xml(new ClientExceptionsData("test", "test")));
    }

    /**
     * @tpTestDetails Send a request and try to read Response for entity which requires special MessageBodyReader,
     *                which is not available. The response contains entity of type Data.
     *                The exception is raised when trying to read response from the server.
     * @tpPassCrit ProcessingException is raised
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("No Message Body Reader Exists Read Entity Test")
    public void noMessageBodyReaderExistsReadEntityTest() throws Exception {
        WebTarget base = client.target(generateURL("/") + "data");
        Response response = base.request().accept("application/xml").get();
        thrown.expect(ProcessingException.class);
        response.readEntity(ClientExceptionsData.class);
    }

    /**
     * @tpTestDetails Send a request and try to read Response for entity which requires special MessageBodyReader,
     *                which is not available. The response contains header with CONTENT_LENGTH zero and no Data entity.
     * @tpPassCrit ProcessingException is raised
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Response With Content Length Zero")
    public void responseWithContentLengthZero() {
        WebTarget base = client.target(generateURL("/") + "empty");
        Response response = base.request().accept("application/xml").get();
        thrown.expect(ProcessingException.class);
        response.readEntity(ClientExceptionsData.class);
    }

    /**
     * @tpTestDetails WebTarget registers ReaderInterceptor and sends entity to a server. Then tries to read
     *                the response, but get interrupted by exception happening in ReaderInterceptor.
     * @tpPassCrit ProcessingException is raised
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Interceptor Throws Exception Test")
    public void interceptorThrowsExceptionTest() {
        WebTarget base = client.target(generateURL("/") + "post");
        Response response = base.register(ClientExceptionsIOExceptionReaderInterceptor.class).request("text/plain")
                .post(Entity.text("data"));
        thrown.expect(ProcessingException.class);
        response.readEntity(ClientExceptionsData.class);
    }

    /**
     * @tpTestDetails WebTarget registers ClientResponseFilter and sends request to a server. The processing of the response
     *                gets
     *                interrupted in the ClientResponseFilter by IOException and processing ends with
     *                ResponseProcessingException.
     * @tpPassCrit ResponseProcessingException is raised
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Response Filter Throws IO Exception Test")
    public void responseFilterThrowsIOExceptionTest() {
        assertThrows(ResponseProcessingException.class, () -> {
            WebTarget base = client.target(generateURL("/") + "get");
            base.register(ClientExceptionsIOExceptionResponseFilter.class).request("text/plain").get();
        });
    }

    /**
     * @tpTestDetails WebTarget registers ClientRequestFilter and sends request to a server. The processing of the request gets
     *                interrupted in the ClientRequestFilter by IOException and processing ends with ProcessingException.
     * @tpPassCrit ProcessingException is raised
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Request Filter Throws IO Exception Test")
    public void requestFilterThrowsIOExceptionTest() {
        assertThrows(ProcessingException.class, () -> {
            WebTarget base = client.target(generateURL("/") + "get");
            base.register(ClientExceptionsIOExceptionRequestFilter.class).request("text/plain").get();
        });
    }

    /**
     * @tpTestDetails WebTarget registers ClientResponseFilter and sends request to a server. The processing of the response
     *                gets interrupted by RuntimeException in the ClientResponseFilter and processing ends with
     *                ResponseProcessingException.
     * @tpPassCrit ResponseProcessingException is raised
     * @tpSince RESTEasy 3.0.24
     */
    @Test
    @DisplayName("Response Filter Throws Runtime Exception Test")
    public void responseFilterThrowsRuntimeExceptionTest() {
        assertThrows(ResponseProcessingException.class, () -> {
            WebTarget base = client.target(generateURL("/") + "get");
            base.register(ClientExceptionsRuntimeExceptionResponseFilter.class).request("text/plain").get();
        });
    }

    /**
     * @tpTestDetails WebTarget registers ClientRequestFilter and sends request to a server. The processing of the request gets
     *                interrupted in the ClientRequestFilter by RuntimeException and processing ends with ProcessingException.
     * @tpPassCrit ProcessingException is raised
     * @tpSince RESTEasy 3.0.24
     */
    @Test
    @DisplayName("Request Filter Throws Runtime Exception Test")
    public void requestFilterThrowsRuntimeExceptionTest() {
        assertThrows(ProcessingException.class, () -> {
            WebTarget base = client.target(generateURL("/") + "get");
            base.register(ClientExceptionsRuntimeExceptionRequestFilter.class).request("text/plain").get();
        });
    }

    /**
     * @tpTestDetails WebTarget registers ClientResponseFilter and sends request to a server. The processing of the response
     *                gets interrupted by ClientExceptionsCustomException in the ClientResponseFilter and processing ends with
     *                ResponseProcessingException.
     * @tpTrackerLink RESTEASY-1591
     * @tpPassCrit ResponseProcessingException is raised
     * @tpSince RESTEasy 3.0.24
     */
    @Test
    @DisplayName("Response Filter Throws Custom Exception Test")
    public void responseFilterThrowsCustomExceptionTest() {
        WebTarget base = client.target(generateURL("/") + "get");
        try {
            base.register(ClientExceptionsCustomExceptionResponseFilter.class).request("text/plain").get();
        } catch (ResponseProcessingException ex) {
            Assertions.assertEquals(ClientExceptionsCustomException.class.getCanonicalName() + ": custom message",
                    ex.getMessage());
        } catch (Throwable ex) {
            Assert.fail("The exception thrown by client was not instance of javax.ws.rs.client.ResponseProcessingException");
        }
    }

    /**
     * @tpTestDetails WebTarget registers ClientRequestFilter and sends request to a server. The processing of the request gets
     *                interrupted in the ClientRequestFilter by ClientExceptionsCustomException and processing ends with
     *                ProcessingException.
     * @tpTrackerLink RESTEASY-1685, RESTEASY-1591
     * @tpPassCrit ProcessingException is raised
     * @tpSince RESTEasy 3.0.24
     */
    @Test
    @DisplayName("Request Filter Throws Custom Exception Test")
    public void requestFilterThrowsCustomExceptionTest() {
        WebTarget base = client.target(generateURL("/") + "get");
        try {
            base.register(ClientExceptionsCustomExceptionRequestFilter.class).request("text/plain").get();
        } catch (ProcessingException ex) {
            Assertions.assertEquals(ClientExceptionsCustomException.class.getCanonicalName() + ": custom message",
                    ex.getMessage());
        } catch (Throwable ex) {
            Assert.fail("The exception thrown by client was not instance of javax.ws.rs.ProcessingException");
        }
    }
}
