package io.quarkus.rest.test.exception;

import java.util.function.Supplier;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.exception.resource.ExceptionHandlingProvider;
import io.quarkus.rest.test.exception.resource.ExceptionHandlingResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Exception Handling Test")
public class ExceptionHandlingTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(ExceptionHandlingTest.class);
            return TestUtil.finishContainerPrepare(war, null, ExceptionHandlingResource.class, ExceptionHandlingProvider.class);
        }
    });

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
        client = null;
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ExceptionHandlingTest.class.getSimpleName());
    }

    @Path("/")
    @DisplayName("Throws Exception Interface")
    public interface ThrowsExceptionInterface {

        @Path("test")
        @POST
        void post() throws Exception;
    }

    /**
     * @tpTestDetails POST request is sent by client via client proxy. The resource on the server throws exception,
     *                which is handled by ExceptionMapper.
     * @tpPassCrit The response with expected Exception text is returned
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Throws Exception")
    public void testThrowsException() throws Exception {
        ThrowsExceptionInterface proxy = client.target(generateURL("/")).proxy(ThrowsExceptionInterface.class);
        try {
            proxy.post();
        } catch (InternalServerErrorException e) {
            Response response = e.getResponse();
            String errorText = response.readEntity(String.class);
            Assertions.assertNotNull(errorText, "Missing the expected exception text");
        }
    }
}
