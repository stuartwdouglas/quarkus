package io.quarkus.rest.test.asynch;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.asynch.resource.AsyncGenericEntityMessageBodyWriter;
import io.quarkus.rest.test.asynch.resource.AsyncGenericEntityResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Asynchronous RESTEasy
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test getting GenericType from return entity.
 * @tpSince RESTEasy 3.7.0
 */
@DisplayName("Async Generic Entity Test")
public class AsyncGenericEntityTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, AsyncGenericEntityMessageBodyWriter.class,
                    AsyncGenericEntityResource.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, AsyncGenericEntityTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test getting GenericType from return entity.
     * @tpSince RESTEasy 3.7.0
     */
    @Test
    @DisplayName("Test Calls")
    public void testCalls() {
        Client client = ClientBuilder.newClient();
        Builder request = client.target(generateURL("/test")).request();
        Response response = request.get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "ok");
        response.close();
        client.close();
    }
}
