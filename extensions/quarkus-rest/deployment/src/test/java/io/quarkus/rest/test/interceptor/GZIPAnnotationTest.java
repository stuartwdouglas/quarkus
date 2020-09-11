package io.quarkus.rest.test.interceptor;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.plugins.interceptors.AcceptEncodingGZIPFilter;
import org.jboss.resteasy.plugins.interceptors.GZIPDecodingInterceptor;
import org.jboss.resteasy.plugins.interceptors.GZIPEncodingInterceptor;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.interceptor.resource.GZIPAnnotationInterface;
import io.quarkus.rest.test.interceptor.resource.GZIPAnnotationResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Interceptor
 * @tpChapter Integration tests
 * @tpTestCaseDetails Tests @GZIP annotation on client (RESTEASY-1265)
 * @tpSince RESTEasy 3.0.20
 */
public class GZIPAnnotationTest {

    static Client client;

    @BeforeClass
    public static void setup() {
        client = ClientBuilder.newClient()
                .register(AcceptEncodingGZIPFilter.class)
                .register(GZIPEncodingInterceptor.class)
                .register(GZIPDecodingInterceptor.class);
    }

    @AfterClass
    public static void close() {
        client.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(GZIPAnnotationInterface.class);
                    war.addAsManifestResource("org/jboss/resteasy/test/client/javax.ws.rs.ext.Providers",
                            "services/javax.ws.rs.ext.Providers");
                    return TestUtil.finishContainerPrepare(war, null, GZIPAnnotationResource.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, GZIPAnnotationTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test that org.jboss.resteasy.plugins.interceptors.ClientContentEncodingAnnotationFilter
     *                and org.jboss.resteasy.plugins.interceptors.AcceptEncodingGZIPFilter
     *                are called on client side
     * @tpSince RESTEasy 3.0.20
     */
    @Test
    public void testGZIP() {
        ResteasyWebTarget target = (ResteasyWebTarget) client.target(generateURL(""));
        GZIPAnnotationInterface resource = target.proxy(GZIPAnnotationInterface.class);
        String s = resource.getFoo("test");
        Assert.assertTrue(s.contains("gzip"));
        Assert.assertTrue(s.substring(s.indexOf("gzip") + 4).contains("gzip"));
    }
}
