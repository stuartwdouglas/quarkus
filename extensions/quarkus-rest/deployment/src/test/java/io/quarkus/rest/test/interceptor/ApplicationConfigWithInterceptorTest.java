package io.quarkus.rest.test.interceptor;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClientBuilder;
import io.quarkus.rest.test.interceptor.resource.AddHeaderContainerResponseFilter;
import io.quarkus.rest.test.interceptor.resource.ApplicationConfigWithInterceptorResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Interceptors
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.20
 */
@DisplayName("Application Config With Interceptor Test")
public class ApplicationConfigWithInterceptorTest {

    private static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(TestUtil.class, PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, ApplicationConfigWithInterceptorResource.class,
                    AddHeaderContainerResponseFilter.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ApplicationConfigWithInterceptorTest.class.getSimpleName());
    }

    @BeforeAll
    public static void before() throws Exception {
        client = QuarkusRestClientBuilder.newClient();
    }

    @AfterAll
    public static void after() throws Exception {
        client.close();
    }

    @Test
    @DisplayName("Test Normal Return")
    public void testNormalReturn() throws Exception {
        doTest("/my/good", 200);
    }

    @Test
    @DisplayName("Test Web Application Exception With Response")
    public void testWebApplicationExceptionWithResponse() throws Exception {
        doTest("/my/bad", 409);
    }

    @Test
    @DisplayName("Test No Content Response")
    public void testNoContentResponse() throws Exception {
        doTest("/my/123", 204, false);
    }

    private void doTest(String path, int expectedStatus) throws Exception {
        doTest(path, expectedStatus, true);
    }

    private void doTest(String path, int expectedStatus, boolean get) throws Exception {
        Builder builder = client.target(generateURL(path)).request();
        Response response = get ? builder.get() : builder.delete();
        Assertions.assertEquals(expectedStatus, response.getStatus());
        Assertions.assertNotNull(response.getHeaderString("custom-header"));
        response.close();
    }
}
