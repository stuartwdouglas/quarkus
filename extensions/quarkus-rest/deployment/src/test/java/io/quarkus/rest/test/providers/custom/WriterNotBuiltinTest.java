package io.quarkus.rest.test.providers.custom;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.custom.resource.ReaderWriterCustomer;
import io.quarkus.rest.test.providers.custom.resource.ReaderWriterResource;
import io.quarkus.rest.test.providers.custom.resource.WriterNotBuiltinTestWriter;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Providers
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Writer Not Builtin Test")
public class WriterNotBuiltinTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(ReaderWriterCustomer.class);
            war.addClass(PortProviderUtil.class);
            Map<String, String> contextParams = new HashMap<>();
            contextParams.put("resteasy.use.builtin.providers", "false");
            // Arquillian in the deployment
            return TestUtil.finishContainerPrepare(war, contextParams, WriterNotBuiltinTestWriter.class,
                    ReaderWriterResource.class);
        }
    });

    /**
     * @tpTestDetails A more complete test for RESTEASY-1.
     *                TestReaderWriter has no type parameter,
     *                so it comes after DefaultPlainText in the built-in ordering.
     *                The fact that TestReaderWriter gets called verifies that
     *                DefaultPlainText gets passed over.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test 1 New")
    public void test1New() throws Exception {
        client = (QuarkusRestClient) ClientBuilder.newClient();
        Response response = client.target(PortProviderUtil.generateURL("/string", WriterNotBuiltinTest.class.getSimpleName()))
                .request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.getStringHeaders().getFirst("content-type"), "text/plain;charset=UTF-8");
        Assertions.assertEquals("hello world", response.readEntity(String.class), "Response contains wrong content");
        Assertions.assertTrue(WriterNotBuiltinTestWriter.used, "Wrong MessageBodyWriter was used");
        client.close();
    }
}
