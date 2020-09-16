package io.quarkus.rest.test.asynch;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.asynch.resource.AsyncTimeoutResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

@DisplayName("Async Timeout Test")
public class AsyncTimeoutTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, AsyncTimeoutResource.class);
        }
    });

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
        return PortProviderUtil.generateURL(path, AsyncTimeoutTest.class.getSimpleName());
    }

    @Test
    @DisplayName("Test Async Time Out")
    public void testAsyncTimeOut() throws Exception {
        WebTarget base = client.target(generateURL("/async"));
        Response response = base.request().get();
        Assertions.assertEquals(response.readEntity(String.class), "Async hello");
        Response timeoutRes = client.target(generateURL("/timeout")).request().get();
        Assertions.assertTrue(timeoutRes.readEntity(String.class).contains("false"), "Wrongly call Timeout Handler");
        response.close();
    }
}
