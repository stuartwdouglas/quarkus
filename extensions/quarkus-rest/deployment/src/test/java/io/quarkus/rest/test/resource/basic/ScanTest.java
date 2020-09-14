package io.quarkus.rest.test.resource.basic;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.resource.basic.resource.ScanProxy;
import io.quarkus.rest.test.resource.basic.resource.ScanResource;
import io.quarkus.rest.test.resource.basic.resource.ScanSubresource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression tests for RESTEASY-263 and RESTEASY-274
 * @tpSince RESTEasy 3.0.16
 */
public class ScanTest {
    private static Client client;

    @BeforeClass
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
    }

    @AfterClass
    public static void after() throws Exception {
        client.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(ScanProxy.class);
                    Map<String, String> contextParams = new HashMap<>();
                    contextParams.put("resteasy.scan", "true");
                    return TestUtil.finishContainerPrepare(war, contextParams, ScanResource.class, ScanSubresource.class);
                }
            });

    /**
     * @tpTestDetails Test with new client
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testNewClient() throws Exception {
        Response response = client.target(PortProviderUtil.generateURL("/test/doit", ScanTest.class.getSimpleName())).request()
                .get();
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("Wrong content of response", "hello world", response.readEntity(String.class));
    }
}
