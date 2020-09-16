package io.quarkus.rest.test.providers.custom;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.custom.resource.CustomContextProviderPreferenceResolver;
import io.quarkus.rest.test.providers.custom.resource.CustomContextProviderPreferenceResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Providers
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for custom ContextProvider preference.
 * @tpSince RESTEasy 3.1.2.Final
 */
@DisplayName("Custom Context Provider Preference Test")
public class CustomContextProviderPreferenceTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, CustomContextProviderPreferenceResolver.class,
                    CustomContextProviderPreferenceResource.class);
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
        return PortProviderUtil.generateURL(path, CustomContextProviderPreferenceTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Client test: RESTEASY-1609
     * @tpSince RESTEasy 3.1.2.Final
     */
    @Test
    @DisplayName("Test Custom Context Provider Preference")
    public void testCustomContextProviderPreference() throws Exception {
        Response response = client.target(generateURL("/test")).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
    }
}
