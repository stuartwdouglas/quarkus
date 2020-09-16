package io.quarkus.rest.test.asynch;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.asynch.resource.AsyncUnhandledExceptionResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Asynchronous RESTEasy
 * @tpChapter Integration tests
 * @tpTestCaseDetails Unhandled exceptions should return 500 status
 * @tpSince RESTEasy 4.0.0
 */
@DisplayName("Async Unhandled Exception Test")
public class AsyncUnhandledExceptionTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, AsyncUnhandledExceptionResource.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, AsyncUnhandledExceptionTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Unhandled exception is thrown from a ReadListener
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    @DisplayName("Test Post")
    public void testPost() {
        Client client = ClientBuilder.newClient();
        try {
            Response response = client.target(generateURL("/listener")).request().post(Entity.entity("aaa", "text/plain"));
            Assertions.assertEquals(500, response.getStatus());
        } finally {
            client.close();
        }
    }

    /**
     * @tpTestDetails Unhandled exception is thrown from a separate thread
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    @DisplayName("Test Get")
    public void testGet() {
        Client client = ClientBuilder.newClient();
        try {
            Response response = client.target(generateURL("/thread")).request().get();
            Assertions.assertEquals(500, response.getStatus());
        } finally {
            client.close();
        }
    }
}
