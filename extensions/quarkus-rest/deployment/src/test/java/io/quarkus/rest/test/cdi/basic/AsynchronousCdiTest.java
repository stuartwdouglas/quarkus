package io.quarkus.rest.test.cdi.basic;

import static io.quarkus.rest.test.Assert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.cdi.basic.resource.AsynchronousResource;
import io.quarkus.rest.test.cdi.basic.resource.AsynchronousStateless;
import io.quarkus.rest.test.cdi.basic.resource.AsynchronousStatelessLocal;
import io.quarkus.rest.test.cdi.util.UtilityProducer;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for asynchronous behavior of RESTEasy with CDI.
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Asynchronous Cdi Test")
public class AsynchronousCdiTest {

    public static final Long DELAY = 5000L;

    protected static final Logger log = Logger.getLogger(AsynchronousCdiTest.class.getName());

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, AsynchronousCdiTest.class.getSimpleName());
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(UtilityProducer.class).addClasses(AsynchronousStatelessLocal.class, AsynchronousStateless.class)
                    .addClasses(AsynchronousResource.class, AsynchronousCdiTest.class);
            return war;
        }
    });

    /**
     * @tpTestDetails Delay is in stateless bean.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Asynch Jax Rs")
    public void testAsynchJaxRs() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget base = client.target(generateURL("/asynch/simple"));
        long start = System.currentTimeMillis();
        Response response = base.request().get();
        assertThat("Response was sent before delay elapsed", System.currentTimeMillis() - start, is(greaterThan(DELAY)));
        assertEquals(200, response.getStatus());
        client.close();
    }

    /**
     * @tpTestDetails Delay is in RESTEasy resource.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Asynch Resource Asynch EJB")
    public void testAsynchResourceAsynchEJB() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget base = client.target(generateURL("/asynch/ejb"));
        long start = System.currentTimeMillis();
        Response response = base.request().get();
        assertThat("Response was sent before delay elapsed", System.currentTimeMillis() - start, is(greaterThan(DELAY)));
        assertEquals(200, response.getStatus());
        client.close();
    }
}
