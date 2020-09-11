package io.quarkus.rest.test.resource.basic;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import io.quarkus.rest.test.ContainerConstants;
import io.quarkus.rest.test.resource.basic.resource.MultipleGetResource;
import org.jboss.resteasy.utils.LogCounter;
import org.jboss.resteasy.utils.PermissionUtil;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import io.quarkus.rest.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.rest.test.simple.TestUtil;
import org.junit.Assert;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FilePermission;
import java.lang.reflect.ReflectPermission;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyPermission;
import java.util.logging.LoggingPermission;

import static org.hamcrest.CoreMatchers.is;
import static io.quarkus.rest.test.ContainerConstants.DEFAULT_CONTAINER_QUALIFIER;

/**
 * Verify that setting resteasy config flag, resteasy_fail_fast to 'true' causes
 * resteasy to report error and not warning.
 * This feature is provided for quarkus.
 */
public class MultipleGetResourceTest {
    static QuarkusRestClient client;

    @Deployment
    public static Archive<?> testReturnValuesDeploy() throws Exception {
        WebArchive war = TestUtil.prepareArchive(MultipleGetResourceTest.class.getSimpleName());
        Map<String, String> contextParam = new HashMap<>();
        contextParam.put(ResteasyContextParameters.RESTEASY_FAIL_FAST_ON_MULTIPLE_RESOURCES_MATCHING, "true");

        return TestUtil.finishContainerPrepare(war, contextParam, MultipleGetResource.class);
    }

    @BeforeClass
    public static void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterClass
    public static void close() {
        client.close();
        client = null;
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, MultipleGetResourceTest.class.getSimpleName());
    }

    @Test
    public void testFailFast() throws Exception {
        LogCounter errorStringLog = new LogCounter("RESTEASY005042",
                false, ContainerConstants.DEFAULT_CONTAINER_QUALIFIER);

        WebTarget base = client.target(generateURL("/api"));
        Response  response = base.request().get();
        Assert.assertEquals(500, response.getStatus());
        response.close();
        Assert.assertThat(errorStringLog.count(), is(2));
    }
}
