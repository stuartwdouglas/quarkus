package io.quarkus.rest.test.exception;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.exception.resource.ExceptionMapperWebRuntimeExceptionMapper;
import io.quarkus.rest.test.exception.resource.ExceptionMapperWebRuntimeExceptionResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 * @tpTestCaseDetails Regression test for RESTEASY-595
 */
@DisplayName("Exception Mapper Web Runtime Exception Test")
public class ExceptionMapperWebRuntimeExceptionTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, ExceptionMapperWebRuntimeExceptionMapper.class,
                    ExceptionMapperWebRuntimeExceptionResource.class);
        }
    });

    /**
     * @tpTestDetails Check ExceptionMapper for WebApplicationException
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Web A Pplication Exception")
    public void testWebAPplicationException() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        WebTarget base = client
                .target(PortProviderUtil.generateURL("/test", ExceptionMapperWebRuntimeExceptionTest.class.getSimpleName()));
        Response response = base.request().get();
        Assertions.assertEquals(Response.Status.PRECONDITION_FAILED.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.getHeaders().getFirst("custom"), "header", "Wrong headers");
        response.close();
        client.close();
    }
}
