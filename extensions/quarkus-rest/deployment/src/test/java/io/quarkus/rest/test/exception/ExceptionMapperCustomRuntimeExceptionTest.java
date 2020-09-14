package io.quarkus.rest.test.exception;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.exception.resource.ExceptionMapperCustomRuntimeCustomMapper;
import io.quarkus.rest.test.exception.resource.ExceptionMapperCustomRuntimeException;
import io.quarkus.rest.test.exception.resource.ExceptionMapperCustomRuntimeMapper;
import io.quarkus.rest.test.exception.resource.ExceptionMapperCustomRuntimeResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 * @tpTestCaseDetails Regression test for RESTEASY-421
 */
public class ExceptionMapperCustomRuntimeExceptionTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(ExceptionMapperCustomRuntimeException.class);
                    return TestUtil.finishContainerPrepare(war, null, ExceptionMapperCustomRuntimeCustomMapper.class,
                            ExceptionMapperCustomRuntimeResource.class, ExceptionMapperCustomRuntimeMapper.class);
                }
            });

    /**
     * @tpTestDetails Check ExceptionMapper for Custom RuntimeException. Check the response contains headers and entity
     *                from custom exception mapper. Using Resteasy client.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testMapperWithQuarkusRestClient() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        WebTarget base = client
                .target(PortProviderUtil.generateURL("/test", ExceptionMapperCustomRuntimeExceptionTest.class.getSimpleName()));
        Response response = base.request().get();
        Assert.assertEquals(Response.Status.PRECONDITION_FAILED.getStatusCode(), response.getStatus());
        Assert.assertEquals("Wrong headers", response.getHeaders().getFirst("custom"), "header");
        Assert.assertEquals("The response doesn't contain the entity from custom exception mapper",
                "My custom message", response.readEntity(String.class));

        response.close();
        client.close();
    }

}
