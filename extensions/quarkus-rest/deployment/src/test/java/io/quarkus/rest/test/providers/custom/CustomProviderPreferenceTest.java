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
import io.quarkus.rest.test.providers.custom.resource.CustomProviderPreferenceUser;
import io.quarkus.rest.test.providers.custom.resource.CustomProviderPreferenceUserBodyWriter;
import io.quarkus.rest.test.providers.custom.resource.CustomProviderPreferenceUserResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Providers
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for custom provider preference.
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Custom Provider Preference Test")
public class CustomProviderPreferenceTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(CustomProviderPreferenceUser.class);
            return TestUtil.finishContainerPrepare(war, null, CustomProviderPreferenceUserResource.class,
                    CustomProviderPreferenceUserBodyWriter.class);
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
        return PortProviderUtil.generateURL(path, CustomProviderPreferenceTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Client test.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Custom Provider Preference")
    public void testCustomProviderPreference() throws Exception {
        Response response = client.target(generateURL("/user")).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("jharting;email@example.com", response.readEntity(String.class), "Wrong content of response");
        response.close();
    }
}
