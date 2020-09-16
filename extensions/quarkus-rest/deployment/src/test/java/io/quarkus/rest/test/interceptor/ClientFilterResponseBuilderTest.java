package io.quarkus.rest.test.interceptor;

import java.util.function.Supplier;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
// import io.quarkus.rest.test.interceptor.resource.ResponseBuilderCustomRequestFilter;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.interceptor.resource.PriorityExecutionResource;
import io.quarkus.rest.test.interceptor.resource.ResponseBuilderCustomResponseFilter;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Demonstrates that a Response filter can process the entity data in a response object
 * and the entity can be properly accessed by the client call.
 */
@DisplayName("Client Filter Response Builder Test")
public class ClientFilterResponseBuilderTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(ResponseBuilderCustomResponseFilter.class, PriorityExecutionResource.class);
            return TestUtil.finishContainerPrepare(war, null);
        }
    });

    static Client client;

    @BeforeEach
    public void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterEach
    public void cleanup() {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ClientFilterResponseBuilderTest.class.getSimpleName());
    }

    @Test
    @DisplayName("Test Response")
    public void testResponse() throws Exception {
        try {
            client.register(ResponseBuilderCustomResponseFilter.class);
            Response response = client.target(generateURL("/test")).request().get();
            Object resultObj = response.getEntity();
            String result = response.readEntity(String.class);
            int status = response.getStatus();
            Assertions.assertEquals(result, "test");
            Assertions.assertEquals(200, status);
        } catch (ProcessingException pe) {
            Assertions.fail(pe.getMessage());
        }
    }
}
