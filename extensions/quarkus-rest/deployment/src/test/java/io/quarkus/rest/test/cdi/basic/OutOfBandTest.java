package io.quarkus.rest.test.cdi.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1049.
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Out Of Band Test")
public class OutOfBandTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return war;
        }
    });

    /**
     * @tpTestDetails JAX-RS resource methods can be called outside the context of a servlet request, leading to NPEs.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Timer Interceptor")
    public void testTimerInterceptor() throws Exception {
        Client client = ClientBuilder.newClient();
        // Schedule timer.
        WebTarget base = client.target(PortProviderUtil.generateURL("/timer/schedule", "RESTEASY-1008"));
        Response response = base.request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
        // Verify timer expired and timer interceptor was executed.
        base = client.target(PortProviderUtil.generateURL("/timer/test", "RESTEASY-1008"));
        response = base.request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
        client.close();
    }
}
