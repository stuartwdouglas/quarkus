package io.quarkus.rest.test.resource.basic;

import static io.quarkus.rest.test.ContainerConstants.DEFAULT_CONTAINER_QUALIFIER;
import static org.hamcrest.CoreMatchers.is;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.utils.LogCounter;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.ContainerConstants;
import io.quarkus.rest.test.resource.basic.resource.MultipleGetResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;

/**
 * Verify that setting resteasy config flag, resteasy_fail_fast to 'true' causes
 * resteasy to report error and not warning.
 * This feature is provided for quarkus.
 */
@DisplayName("Multiple Get Resource Test")
public class MultipleGetResourceTest {

    static QuarkusRestClient client;

    @Deployment
    public static Archive<?> testReturnValuesDeploy() throws Exception {
        WebArchive war = TestUtil.prepareArchive(MultipleGetResourceTest.class.getSimpleName());
        Map<String, String> contextParam = new HashMap<>();
        contextParam.put(ResteasyContextParameters.RESTEASY_FAIL_FAST_ON_MULTIPLE_RESOURCES_MATCHING, "true");
        return TestUtil.finishContainerPrepare(war, contextParam, MultipleGetResource.class);
    }

    @BeforeAll
    public static void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterAll
    public static void close() {
        client.close();
        client = null;
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, MultipleGetResourceTest.class.getSimpleName());
    }

    @Test
    @DisplayName("Test Fail Fast")
    public void testFailFast() throws Exception {
        LogCounter errorStringLog = new LogCounter("RESTEASY005042", false, ContainerConstants.DEFAULT_CONTAINER_QUALIFIER);
        WebTarget base = client.target(generateURL("/api"));
        Response response = base.request().get();
        Assertions.assertEquals(500, response.getStatus());
        response.close();
        Assert.assertThat(errorStringLog.count(), is(2));
    }
}
