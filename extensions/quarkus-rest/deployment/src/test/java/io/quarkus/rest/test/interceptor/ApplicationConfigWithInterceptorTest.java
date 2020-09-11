package io.quarkus.rest.test.interceptor;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
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
public class ApplicationConfigWithInterceptorTest {
    private static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
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

    @BeforeClass
    public static void before() throws Exception {
        client = QuarkusRestClientBuilder.newClient();
    }

    @AfterClass
    public static void after() throws Exception {
        client.close();
    }

    @Test
    public void testNormalReturn() throws Exception {
        doTest("/my/good", 200);
    }

    @Test
    public void testWebApplicationExceptionWithResponse() throws Exception {
        doTest("/my/bad", 409);
    }

    @Test
    public void testNoContentResponse() throws Exception {
        doTest("/my/123", 204, false);
    }

    private void doTest(String path, int expectedStatus) throws Exception {
        doTest(path, expectedStatus, true);
    }

    private void doTest(String path, int expectedStatus, boolean get) throws Exception {
        Builder builder = client.target(generateURL(path)).request();
        Response response = get ? builder.get() : builder.delete();
        Assert.assertEquals(expectedStatus, response.getStatus());
        Assert.assertNotNull(response.getHeaderString("custom-header"));
        response.close();
    }
}
