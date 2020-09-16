package io.quarkus.rest.test.client.exception;

import java.util.function.Supplier;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.client.exception.resource.UnauthorizedExceptionInterface;
import io.quarkus.rest.test.client.exception.resource.UnauthorizedExceptionResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpSince RESTEasy 3.0.16
 * @tpTestCaseDetails Regression test for RESTEASY-435
 */
@DisplayName("Unauthorized Exception Test")
public class UnauthorizedExceptionTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(UnauthorizedExceptionInterface.class);
            return TestUtil.finishContainerPrepare(war, null, UnauthorizedExceptionResource.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, UnauthorizedExceptionTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Check thrown exception on client side.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Me")
    public void testMe() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        UnauthorizedExceptionInterface proxy = client.target(generateURL("")).proxy(UnauthorizedExceptionInterface.class);
        try {
            proxy.postIt("hello");
            Assertions.fail();
        } catch (NotAuthorizedException e) {
            Assertions.assertEquals(Status.UNAUTHORIZED.getStatusCode(), e.getResponse().getStatus());
        }
        client.close();
    }
}
