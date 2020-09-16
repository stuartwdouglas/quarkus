package io.quarkus.rest.test.providers.jackson2.jsonfilter;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.Jackson2Product;
import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.Jackson2Resource;
import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.JsonFilterModifierConditionalWriterInterceptor;
import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.ObjectFilterModifierConditional;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jackson2 provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Filters fields from json object. Sets ObjectWriterModifier in the interceptor.
 *                    The filter filters field of Jackson2Product pojo upon value if its 'id' field. Pojo with id value < 0 is
 *                    filtered
 *                    out and not returned in the response. See http://www.baeldung.com/jackson-serialize-field-custom-criteria
 * @tpSince RESTEasy 3.1.0
 */
@DisplayName("Json Filter With Interceptor Conditional Filter Test")
public class JsonFilterWithInterceptorConditionalFilterTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(Jackson2Product.class, ObjectFilterModifierConditional.class);
            war.addAsManifestResource(new StringAsset(
                    "Manifest-Version: 1.0\n" + "Dependencies: com.fasterxml.jackson.jaxrs.jackson-jaxrs-json-provider\n"),
                    "MANIFEST.MF");
            return TestUtil.finishContainerPrepare(war, null, Jackson2Resource.class,
                    JsonFilterModifierConditionalWriterInterceptor.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, JsonFilterWithInterceptorConditionalFilterTest.class.getSimpleName());
    }

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Json field id is filtered out
     * @tpSince RESTEasy 3.1.0
     */
    @Test
    @DisplayName("Test Jackson Conditional String Property Filtered")
    public void testJacksonConditionalStringPropertyFiltered() throws Exception {
        WebTarget target = client.target(generateURL("/products/-1"));
        Response response = target.request().get();
        response.bufferEntity();
        Assertions.assertTrue(
                !response.readEntity(String.class).contains("id") && response.readEntity(String.class).contains("name"),
                "Conditional filter doesn't work");
    }

    /**
     * @tpTestDetails Json field id is not filtered
     * @tpSince RESTEasy 3.1.0
     */
    @Test
    @DisplayName("Test Jackson Conditional String Property Not Filtered")
    public void testJacksonConditionalStringPropertyNotFiltered() throws Exception {
        WebTarget target = client.target(generateURL("/products/333"));
        Response response = target.request().get();
        response.bufferEntity();
        Assertions.assertTrue(
                response.readEntity(String.class).contains("id") && response.readEntity(String.class).contains("name"),
                "Conditional filter doesn't work");
    }
}
