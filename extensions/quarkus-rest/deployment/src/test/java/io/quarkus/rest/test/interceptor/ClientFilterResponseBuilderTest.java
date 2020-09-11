package io.quarkus.rest.test.interceptor;

import java.util.function.Supplier;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
// import io.quarkus.rest.test.interceptor.resource.ResponseBuilderCustomRequestFilter;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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
public class ClientFilterResponseBuilderTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(ResponseBuilderCustomResponseFilter.class,
                            PriorityExecutionResource.class);
                    return TestUtil.finishContainerPrepare(war, null);
                }
            });

    static Client client;

    @Before
    public void setup() {
        client = ClientBuilder.newClient();
    }

    @After
    public void cleanup() {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path,
                ClientFilterResponseBuilderTest.class.getSimpleName());
    }

    @Test
    public void testResponse() throws Exception {
        try {
            client.register(ResponseBuilderCustomResponseFilter.class);
            Response response = client.target(generateURL("/test")).request().get();
            Object resultObj = response.getEntity();
            String result = response.readEntity(String.class);
            int status = response.getStatus();
            Assert.assertEquals("test", result);
            Assert.assertEquals(200, status);
        } catch (ProcessingException pe) {
            Assert.fail(pe.getMessage());
        }
    }
}
