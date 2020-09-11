package io.quarkus.rest.test.interceptor;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
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
public class ReaderInterceptorContextTest {
    private static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
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

    @BeforeClass
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
    }

    @AfterClass
    public static void after() throws Exception {
        client.close();
    }

    @Test
    public void testInterceptorHeaderMap() throws Exception {
        Response response = client.target(generateURL("/post")).request().post(Entity.entity("dummy", "text/plain"));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("123789", response.readEntity(String.class));
    }
}
