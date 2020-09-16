package io.quarkus.rest.test.cdi.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.cdi.basic.resource.SingletonLocalIF;
import io.quarkus.rest.test.cdi.basic.resource.SingletonRootResource;
import io.quarkus.rest.test.cdi.basic.resource.SingletonSubResource;
import io.quarkus.rest.test.cdi.basic.resource.SingletonTestBean;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for Singleton beans
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Singleton Test")
public class SingletonTest {

    static Client client;

    protected static final Logger logger = Logger.getLogger(SingletonTest.class.getName());

    @BeforeAll
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void close() {
        client.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(SingletonLocalIF.class, SingletonSubResource.class, SingletonTestBean.class);
            return TestUtil.finishContainerPrepare(war, null, SingletonRootResource.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, SingletonTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Three requests for singleton bean
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Singleton")
    public void testSingleton() throws Exception {
        WebTarget base = client.target(generateURL("/root"));
        String value = base.path("sub").request().get(String.class);
        Assertions.assertEquals("hello", value, "Wrong content of response");
        value = base.path("injected").request().get(String.class);
        Assertions.assertEquals("true", value, "Wrong content of response");
        value = base.path("intfsub").request().get(String.class);
        logger.info(value);
        Response response = base.path("exception").request().get();
        Assertions.assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
    }
}
