package io.quarkus.rest.test.core.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.core.basic.resource.ApplicationConfig;
import io.quarkus.rest.test.core.basic.resource.ApplicationConfigInjectionResource;
import io.quarkus.rest.test.core.basic.resource.ApplicationConfigInterface;
import io.quarkus.rest.test.core.basic.resource.ApplicationConfigQuotedTextWriter;
import io.quarkus.rest.test.core.basic.resource.ApplicationConfigResource;
import io.quarkus.rest.test.core.basic.resource.ApplicationConfigService;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Configuration
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for custom Application class
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Application Config Test")
public class ApplicationConfigTest {

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(ApplicationConfig.class, ApplicationConfigInjectionResource.class, ApplicationConfigInterface.class,
                    ApplicationConfigQuotedTextWriter.class, ApplicationConfigResource.class, ApplicationConfigService.class);
            return war;
        }
    });

    @BeforeAll
    public static void init() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ApplicationConfigTest.class.getSimpleName());
    }

    private void basicTest(String uri, String body) {
        Response response = client.target(uri).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Test base resource
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test It")
    public void testIt() {
        basicTest(generateURL("/my"), "\"hello\"");
        basicTest(generateURL("/myinterface"), "hello");
    }

    /**
     * @tpTestDetails Injection test
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Field Injection")
    public void testFieldInjection() {
        basicTest(generateURL("/injection/field"), "true");
    }

    /**
     * @tpTestDetails Setter injection test
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Setter Injection")
    public void testSetterInjection() {
        basicTest(generateURL("/injection/setter"), "true");
    }

    /**
     * @tpTestDetails Setter injection test
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Constructor Injection")
    public void testConstructorInjection() {
        basicTest(generateURL("/injection/constructor"), "true");
    }
}
