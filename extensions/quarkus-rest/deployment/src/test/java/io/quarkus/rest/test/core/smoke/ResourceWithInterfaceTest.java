package io.quarkus.rest.test.core.smoke;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.core.smoke.resource.ResourceWithInterfaceResourceWithInterface;
import io.quarkus.rest.test.core.smoke.resource.ResourceWithInterfaceSimpleClient;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Smoke tests for jaxrs
 * @tpChapter Integration tests
 * @tpTestCaseDetails Smoke test for resource with interface.
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Resource With Interface Test")
public class ResourceWithInterfaceTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(ResourceWithInterfaceSimpleClient.class);
            return TestUtil.finishContainerPrepare(war, null, ResourceWithInterfaceResourceWithInterface.class);
        }
    });

    /**
     * @tpTestDetails Check result from resource with interface.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test No Defaults Resource")
    public void testNoDefaultsResource() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        ResourceWithInterfaceSimpleClient proxy = client
                .target(PortProviderUtil.generateBaseUrl(ResourceWithInterfaceTest.class.getSimpleName()))
                .proxy(ResourceWithInterfaceSimpleClient.class);
        Assertions.assertEquals("basic", proxy.getBasic(), "Wrong client answer.");
        proxy.putBasic("hello world");
        Assertions.assertEquals("hello world", proxy.getQueryParam("hello world"), "Wrong client answer.");
        Assertions.assertEquals(1234, proxy.getUriParam(1234), "Wrong client answer.");
        client.close();
    }
}
