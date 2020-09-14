package io.quarkus.rest.test.exception;

import java.util.function.Supplier;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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
public class ExceptionHandlingTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(ExceptionHandlingTest.class);
                    return TestUtil.finishContainerPrepare(war, null, ExceptionHandlingResource.class,
                            ExceptionHandlingProvider.class);
                }
            });

    @Before
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @After
    public void after() throws Exception {
        client.close();
        client = null;
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ExceptionHandlingTest.class.getSimpleName());
    }

    @Path("/")
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
    public void testThrowsException() throws Exception {

        ThrowsExceptionInterface proxy = client.target(generateURL("/")).proxy(ThrowsExceptionInterface.class);
        try {
            proxy.post();
        } catch (InternalServerErrorException e) {
            Response response = e.getResponse();
            String errorText = response.readEntity(String.class);
            Assert.assertNotNull("Missing the expected exception text", errorText);
        }

    }

}
