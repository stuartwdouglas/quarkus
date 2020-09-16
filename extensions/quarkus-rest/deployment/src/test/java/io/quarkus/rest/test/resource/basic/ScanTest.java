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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
@DisplayName("Scan Test")
public class ScanTest {

    private static Client client;

    @BeforeAll
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void after() throws Exception {
        client.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

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
    @DisplayName("Test New Client")
    public void testNewClient() throws Exception {
        Response response = client.target(PortProviderUtil.generateURL("/test/doit", ScanTest.class.getSimpleName())).request()
                .get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("hello world", response.readEntity(String.class), "Wrong content of response");
    }
}
