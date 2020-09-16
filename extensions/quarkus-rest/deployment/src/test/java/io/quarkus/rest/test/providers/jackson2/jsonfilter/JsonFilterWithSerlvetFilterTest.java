package io.quarkus.rest.test.providers.jackson2.jsonfilter;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.Jackson2Product;
import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.Jackson2Resource;
import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.ObjectFilterModifier;
import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.ObjectWriterModifierFilter;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 * @tpSubChapter Jackson2 provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.1.0
 */
@DisplayName("Json Filter With Serlvet Filter Test")
public class JsonFilterWithSerlvetFilterTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(ObjectFilterModifier.class, Jackson2Product.class, ObjectWriterModifierFilter.class);
            war.addAsManifestResource(new StringAsset(
                    "Manifest-Version: 1.0\n" + "Dependencies: com.fasterxml.jackson.jaxrs.jackson-jaxrs-json-provider\n"),
                    "MANIFEST.MF");
            // war.addAsWebInfResource(JsonFilterWithSerlvetFilterTest.class.getPackage(), "web.xml", "web.xml");
            return TestUtil.finishContainerPrepare(war, null, Jackson2Resource.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, JsonFilterWithSerlvetFilterTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Filters fields from json response entity. Specifies the filter implementation class in web.xml.
     * @tpSince RESTEasy 3.1.0
     */
    @Test
    @DisplayName("Test Jackson String")
    public void testJacksonString() throws Exception {
        Client client = (QuarkusRestClient) ClientBuilder.newClient();
        WebTarget target = client.target(generateURL("/products/333"));
        Response response = target.request().get();
        response.bufferEntity();
        Assertions.assertTrue(
                !response.readEntity(String.class).contains("id") && response.readEntity(String.class).contains("name"),
                "filter doesn't work");
        client.close();
    }
}
