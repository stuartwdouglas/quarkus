package io.quarkus.rest.test.providers.custom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.custom.resource.CustomClientConstrainedFeature;
import io.quarkus.rest.test.providers.custom.resource.CustomConstrainedFeatureResource;
import io.quarkus.rest.test.providers.custom.resource.CustomServerConstrainedFeature;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Core
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.6.1
 * @tpTestCaseDetails Regression test for RESTEASY-1861
 */
public class CustomConstrainedFeatureTest {

    private static final String TEST_URI = generateURL("/test-custom-feature");
    private static final Logger LOGGER = Logger.getLogger(CustomConstrainedFeatureTest.class.getName());
    private static final String CUSTOM_PROVIDERS_FILENAME = "CustomConstrainedFeature.Providers";

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addAsResource(CustomConstrainedFeatureTest.class.getPackage(), CUSTOM_PROVIDERS_FILENAME,
                            "META-INF/services/javax.ws.rs.ext.Providers");
                    return TestUtil.finishContainerPrepare(war, null, CustomConstrainedFeatureResource.class,
                            CustomServerConstrainedFeature.class, CustomClientConstrainedFeature.class);
                }
            });

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, CustomConstrainedFeatureTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Call client with restricted feature for server runtime.
     * @tpSince RESTEasy 3.6.1
     */
    @Test
    public void testClientCall() {
        CustomServerConstrainedFeature.reset();
        CustomClientConstrainedFeature.reset();
        // This will register always in SERVER runtime
        // ResteasyProviderFactory providerFactory = ResteasyProviderFactory.newInstance();
        // providerFactory.register(CustomClientConstrainedFeature.class);
        // providerFactory.register(CustomServerConstrainedFeature.class);
        // QuarkusRestClientImpl client = new QuarkusRestClientBuilderImpl().build();
        // the line below does the same as if there is providers file in META-INF/services/javax.ws.rs.ext.Providers
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newBuilder().register(CustomClientConstrainedFeature.class)
                .register(CustomServerConstrainedFeature.class).build();
        assertTrue(CustomConstrainedFeatureResource.ERROR_CLIENT_FEATURE, CustomClientConstrainedFeature.wasInvoked());
        assertFalse(CustomConstrainedFeatureResource.ERROR_SERVER_FEATURE, CustomServerConstrainedFeature.wasInvoked());
        Response response = client.target(TEST_URI).request().get();
        LOGGER.info("Response from server: {}", response.readEntity(String.class));
        // server must return 200 if only registered feature was for server runtime
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        client.close();
    }
}
