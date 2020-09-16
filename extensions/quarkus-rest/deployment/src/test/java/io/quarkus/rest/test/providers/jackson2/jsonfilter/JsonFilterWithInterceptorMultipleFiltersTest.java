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

import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.Jackson2Person;
import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.Jackson2PersonResource;
import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.JsonFilterModifierMultipleWriteInterceptor;
import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.ObjectFilterModifierMultiple;
import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.PersonType;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jackson2 provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Filters fields from json object. Sets ObjectWriterModifier in the interceptor.
 *                    The filter filters field personType of Jackson2Person pojo. The ObjectWriterModifier has multiple filters
 *                    registered.
 *                    Only one is set to for Json2Person pojo.
 * @tpSince RESTEasy 3.1.0
 */
@DisplayName("Json Filter With Interceptor Multiple Filters Test")
public class JsonFilterWithInterceptorMultipleFiltersTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(Jackson2Person.class, PersonType.class, ObjectFilterModifierMultiple.class);
            war.addAsManifestResource(new StringAsset(
                    "Manifest-Version: 1.0\n" + "Dependencies: com.fasterxml.jackson.jaxrs.jackson-jaxrs-json-provider\n"),
                    "MANIFEST.MF");
            return TestUtil.finishContainerPrepare(war, null, Jackson2PersonResource.class,
                    JsonFilterModifierMultipleWriteInterceptor.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, JsonFilterWithInterceptorMultipleFiltersTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Correct filter is used when multiple filters available
     * @tpSince RESTEasy 3.1.0
     */
    @Test
    @DisplayName("Test Jackson String 2")
    public void testJacksonString2() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(generateURL("/person/333"));
        Response response = target.request().get();
        response.bufferEntity();
        Assertions.assertTrue(!response.readEntity(String.class).contains("id")
                && !response.readEntity(String.class).contains("name") && !response.readEntity(String.class).contains("address")
                && response.readEntity(String.class).contains("personType"), "Multiple filter doesn't work");
        client.close();
    }
}
