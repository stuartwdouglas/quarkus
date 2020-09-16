package io.quarkus.rest.test.interceptor;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.jboss.resteasy.plugins.interceptors.AcceptEncodingGZIPFilter;
import org.jboss.resteasy.plugins.interceptors.GZIPDecodingInterceptor;
import org.jboss.resteasy.plugins.interceptors.GZIPEncodingInterceptor;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
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
@DisplayName("Gzip Annotation Test")
public class GZIPAnnotationTest {

    static Client client;

    @BeforeAll
    public static void setup() {
        client = ClientBuilder.newClient().register(AcceptEncodingGZIPFilter.class).register(GZIPEncodingInterceptor.class)
                .register(GZIPDecodingInterceptor.class);
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
    @DisplayName("Test GZIP")
    public void testGZIP() {
        QuarkusRestWebTarget target = (QuarkusRestWebTarget) client.target(generateURL(""));
        GZIPAnnotationInterface resource = target.proxy(GZIPAnnotationInterface.class);
        String s = resource.getFoo("test");
        Assertions.assertTrue(s.contains("gzip"));
        Assertions.assertTrue(s.substring(s.indexOf("gzip") + 4).contains("gzip"));
    }
}
