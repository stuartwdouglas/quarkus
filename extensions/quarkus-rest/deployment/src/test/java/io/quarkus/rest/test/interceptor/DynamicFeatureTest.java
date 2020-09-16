package io.quarkus.rest.test.interceptor;

import static io.quarkus.rest.test.ContainerConstants.DEFAULT_CONTAINER_QUALIFIER;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.resteasy.utils.LogCounter;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.rest.test.interceptor.resource.AddDynamicFeature;
import io.quarkus.rest.test.interceptor.resource.DynamicFeatureResource;
import io.quarkus.rest.test.interceptor.resource.GreetingInterceptor;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;

/**
 * @tpSubChapter Resteasy-interceptor
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for DynamicFeature
 * @tpSince RESTEasy 3.8.0
 */
@DisplayName("Dynamic Feature Test")
public class DynamicFeatureTest {

    private static Client client;

    private static LogCounter logCounter;

    @Deployment
    public static Archive<?> deploy() {
        // LogCounter needs to be created before deployment because it can only count logs written after its creation
        logCounter = new LogCounter("This should be happening exactly once", false, DEFAULT_CONTAINER_QUALIFIER);
        WebArchive war = TestUtil.prepareArchive(DynamicFeatureTest.class.getSimpleName());
        war.addClasses(GreetingInterceptor.class);
        return TestUtil.finishContainerPrepare(war, null, DynamicFeatureResource.class, AddDynamicFeature.class);
    }

    @BeforeAll
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void cleanup() {
        client.close();
    }

    /**
     * @tpTestDetails This test checks that:
     *                - dynamic features are processed at application deployment time
     *                - dynamic features for filters and interceptors are resolved once for each resource method
     * @tpSince RESTEasy 3.8.0
     */
    @Test
    @InSequence(1)
    @DisplayName("Test Dynamic Feature Processing")
    public void testDynamicFeatureProcessing() {
        int counter = logCounter.count();
        Assertions.assertNotEquals(0, counter, "Dynamic features were not processed at application deployment time");
        Assertions.assertEquals(1, counter,
                "Dynamic features for filters and interceptors should be resolved only once for each resource method");
    }

    /**
     * @tpTestDetails This test checks that dynamic feature works by checking that interceptor was used.
     * @tpSince RESTEasy 3.8.0
     */
    @Test
    @InSequence(2)
    @DisplayName("Test Interceptor")
    public void testInterceptor() {
        WebTarget target = client
                .target(PortProviderUtil.generateURL("/dynamic-feature/hello", DynamicFeatureTest.class.getSimpleName()));
        Entity<String> entity = Entity.entity("Tomas", MediaType.TEXT_PLAIN);
        String response = target.request().post(entity, String.class);
        Assertions.assertEquals(response, "Hello Tomas !");
    }
}
