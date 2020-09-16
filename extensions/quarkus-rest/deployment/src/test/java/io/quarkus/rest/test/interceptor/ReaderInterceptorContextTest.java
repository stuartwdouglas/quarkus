package io.quarkus.rest.test.interceptor;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.interceptor.resource.ReaderInterceptorContextInterceptor;
import io.quarkus.rest.test.interceptor.resource.ReaderInterceptorContextResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Verify ReaderInterceptorContext.getHeaders() returns mutable map: RESTEASY-2298.
 * @tpChapter Integration tests
 * @tpSince RESTEasy 4.2.0
 */
@DisplayName("Reader Interceptor Context Test")
public class ReaderInterceptorContextTest {

    private static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, ReaderInterceptorContextInterceptor.class,
                    ReaderInterceptorContextResource.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ReaderInterceptorContextTest.class.getSimpleName());
    }

    @BeforeAll
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void after() throws Exception {
        client.close();
    }

    @Test
    @DisplayName("Test Interceptor Header Map")
    public void testInterceptorHeaderMap() throws Exception {
        Response response = client.target(generateURL("/post")).request().post(Entity.entity("dummy", "text/plain"));
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "123789");
    }
}
