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
import io.quarkus.rest.test.microprofile.config.resource.MicroProfileConfigUseGlobalResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter MicroProfile Config: ServletConfig with useGlobal and multiple servlets
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression tests for RESTEASY-2266
 * @tpSince RESTEasy 4.1.0
 */
public class MicroProfileConfigUseGlobalTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, MicroProfileConfigUseGlobalResource.class);
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
        return PortProviderUtil.generateURL(path, MicroProfileConfigUseGlobalTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails
     * @tpSince RESTEasy 4.1.0
     */
    @Test
    public void testMultipleAppsUseGlobal() throws Exception {
        Response response = client.target(generateURL("/app1/prefix")).request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("/app1", response.readEntity(String.class));
        response = client.target(generateURL("/app2/prefix")).request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("/app2", response.readEntity(String.class));
    }
}
