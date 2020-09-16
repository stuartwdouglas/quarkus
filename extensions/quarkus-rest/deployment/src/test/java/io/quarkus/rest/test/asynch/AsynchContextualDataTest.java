package io.quarkus.rest.test.asynch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClientBuilder;
import io.quarkus.rest.test.asynch.resource.AsynchContextualDataProduct;
import io.quarkus.rest.test.asynch.resource.AsynchContextualDataResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Asynchronous RESTEasy: RESTEASY-1225
 * @tpChapter Integration tests
 * @tpTestCaseDetails Tests that Providers context is not discarded prematurely
 * @tpSince RESTEasy 3.1.1.Final
 */
@DisplayName("Asynch Contextual Data Test")
public class AsynchContextualDataTest {

    public static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(AsynchContextualDataProduct.class);
            List<Class<?>> singletons = new ArrayList<Class<?>>();
            singletons.add(AsynchContextualDataResource.class);
            Map<String, String> contextParam = new HashMap<>();
            contextParam.put(ResteasyContextParameters.RESTEASY_PREFER_JACKSON_OVER_JSONB, "true");
            return TestUtil.finishContainerPrepare(war, contextParam, singletons);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, AsynchContextualDataTest.class.getSimpleName());
    }

    @BeforeAll
    public static void initClient() {
        client = ((QuarkusRestClientBuilder) ClientBuilder.newBuilder()).connectionPoolSize(10).build();
    }

    @AfterAll
    public static void closeClient() {
        client.close();
    }

    /**
     * @tpTestDetails Test stack handling of context data map
     * @tpSince RESTEasy 3.1.1.Final
     */
    @Test
    @DisplayName("Test Contextual Data")
    public void testContextualData() throws Exception {
        String id = "334";
        // Start the request to the waiting endpoint, but don't block
        WebTarget target = client.target(generateURL("/products/wait/" + id));
        Future<Response> response = target.request().async().get();
        // Let the server set the resumable field, timing thing!
        Thread.sleep(3000);
        // While the other request is waiting, fire off a request to /res/ which will allow the other request to complete
        WebTarget resTarget = client.target(generateURL("/products/res/" + id));
        Response resResponse = resTarget.request().get();
        String entity = response.get().readEntity(String.class);
        String resEntity = resResponse.readEntity(String.class);
        Assertions.assertEquals(200, response.get().getStatus());
        Assertions.assertEquals("{\"name\":\"Iphone\",\"id\":" + id + "}", entity);
        Assertions.assertEquals(200, resResponse.getStatus());
        Assertions.assertEquals("{\"name\":\"Nexus 7\",\"id\":" + id + "}", resEntity);
        response.get().close();
        resResponse.close();
    }
}
