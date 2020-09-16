package io.quarkus.rest.test.interceptor;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
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

import io.quarkus.rest.test.interceptor.resource.InterceptorStreamCustom;
import io.quarkus.rest.test.interceptor.resource.InterceptorStreamResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Interceptors
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.1.0
 * @tpTestCaseDetails Change InputStream and OutputStream in ReaderInterceptor and WriterInterceptor
 */
@DisplayName("Interceptor Stream Test")
public class InterceptorStreamTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, InterceptorStreamResource.class, InterceptorStreamCustom.class);
        }
    });

    static Client client;

    @BeforeEach
    public void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterEach
    public void cleanup() {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, InterceptorStreamTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Use ReaderInterceptor and WriterInterceptor together
     * @tpSince RESTEasy 3.1.0
     */
    @Test
    @DisplayName("Test Priority")
    public void testPriority() throws Exception {
        Response response = client.target(generateURL("/test")).request().post(Entity.text("test"));
        response.bufferEntity();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus(),
                "Wrong response status, interceptors don't work correctly");
        Assertions.assertEquals("writer_interceptor_testtest", response.readEntity(String.class),
                "Wrong content of response, interceptors don't work correctly");
    }
}
