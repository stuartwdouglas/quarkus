package io.quarkus.rest.test.interceptor;

import java.util.function.Supplier;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.interceptor.resource.CustomException;
import io.quarkus.rest.test.interceptor.resource.ThrowCustomExceptionResponseFilter;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Interceptors
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.21
 * @tpTestCaseDetails Throw custom exception from a ClientResponseFilter [RESTEASY-1591]
 */
@DisplayName("Response Filter Custom Exception Test")
public class ResponseFilterCustomExceptionTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(TestUtil.class, PortProviderUtil.class);
            war.addClasses(CustomException.class);
            return TestUtil.finishContainerPrepare(war, null, ThrowCustomExceptionResponseFilter.class);
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
        return PortProviderUtil.generateURL(path, ResponseFilterCustomExceptionTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Use ClientResponseFilter
     * @tpSince RESTEasy 3.0.21
     */
    @Test
    @DisplayName("Test Throw Custom Exception")
    public void testThrowCustomException() throws Exception {
        try {
            client.register(ThrowCustomExceptionResponseFilter.class);
            client.target(generateURL("/testCustomException")).request().post(Entity.text("testCustomException"));
        } catch (ProcessingException pe) {
            Assertions.assertEquals(CustomException.class, pe.getCause().getClass());
        }
    }
}
