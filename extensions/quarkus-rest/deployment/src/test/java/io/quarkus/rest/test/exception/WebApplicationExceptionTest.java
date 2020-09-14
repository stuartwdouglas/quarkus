package io.quarkus.rest.test.exception;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.exception.resource.WebApplicationExceptionResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Exceptions
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for javax.ws.rs.WebApplicationException class
 * @tpSince RESTEasy 3.0.16
 */
public class WebApplicationExceptionTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, WebApplicationExceptionResource.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, WebApplicationExceptionTest.class.getSimpleName());
    }

    private void basicTest(String path, int code) {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        WebTarget base = client.target(generateURL(path));
        Response response = base.request().get();
        Assert.assertEquals(code, response.getStatus());
        response.close();
        client.close();
    }

    /**
     * @tpTestDetails Test for exception without error entity
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testException() {
        basicTest("/exception", Status.UNAUTHORIZED);
    }

    /**
     * @tpTestDetails Test for exception with error entity.
     *                Regression test for RESTEASY-24
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testExceptionWithEntity() {
        basicTest("/exception/entity", Status.UNAUTHORIZED);
    }

}
