package io.quarkus.rest.test.microprofile.config;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.microprofile.config.resource.MicroProfileConfigResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter MicroProfile Config
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression tests for RESTEASY-2131
 * @tpSince RESTEasy 4.0.0
 */
public class MicroProfileConfigFilterTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, MicroProfileConfigResource.class);
                }
            });

    @BeforeClass
    public static void before() throws Exception {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterClass
    public static void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, MicroProfileConfigFilterTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Verify system variables are accessible and have highest priority; get Config programmatically.
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testSystemProgrammatic() throws Exception {
        Response response = client.target(generateURL("/system/prog")).request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("system-system", response.readEntity(String.class));
    }

    /**
     * @tpTestDetails Verify system variables are accessible and have highest priority; get Config by injection.
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testSystemInject() throws Exception {
        Response response = client.target(generateURL("/system/inject")).request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("system-system", response.readEntity(String.class));
    }

    /**
     * @tpTestDetails Verify web.xml filter params are accessible and have higher priority than context params; get Config
     *                programmatically.
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testFilterProgrammatic() throws Exception {
        Response response = client.target(generateURL("/filter/prog")).request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("filter-filter", response.readEntity(String.class));
    }

    /**
     * @tpTestDetails Verify web.xml filter params are accessible and have higher priority than context params; get Config by
     *                injection.
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testFilterInject() throws Exception {
        Response response = client.target(generateURL("/filter/inject")).request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("filter-filter", response.readEntity(String.class));
    }

    /**
     * @tpTestDetails Verify web.xml context params are accessible; get Config programmatically.
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testContextProgrammatic() throws Exception {
        Response response = client.target(generateURL("/context/prog")).request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("context-context", response.readEntity(String.class));
    }

    /**
     * @tpTestDetails Verify web.xml context params are accessible; get Config by injection.
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testContextInject() throws Exception {
        Response response = client.target(generateURL("/context/inject")).request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("context-context", response.readEntity(String.class));
    }

    /**
     * @tpTestDetails Verify former context parameter "resteasy.add.charset" is overridden by system property.
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testActualContextParameter() throws Exception {
        Response response = client.target(generateURL("/actual")).request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("text/plain", response.getHeaderString("Content-Type"));
    }
}
