package io.quarkus.rest.test.providers.custom;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.custom.resource.CustomValueInjectorHello;
import io.quarkus.rest.test.providers.custom.resource.CustomValueInjectorHelloResource;
import io.quarkus.rest.test.providers.custom.resource.CustomValueInjectorInjectorFactoryImpl;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Providers
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for custom value injector.
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Custom Value Injector Test")
public class CustomValueInjectorTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(CustomValueInjectorHello.class);
            return TestUtil.finishContainerPrepare(war, null, CustomValueInjectorHelloResource.class,
                    CustomValueInjectorInjectorFactoryImpl.class);
        }
    });

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, CustomValueInjectorTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Client test.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Custom Injector Factory")
    public void testCustomInjectorFactory() throws Exception {
        String result = client.target(generateURL("/")).request().get(String.class);
        Assertions.assertEquals("world", result, "Response has wrong content");
    }
}
