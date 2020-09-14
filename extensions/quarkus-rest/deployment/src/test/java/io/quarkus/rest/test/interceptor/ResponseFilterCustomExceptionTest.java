package io.quarkus.rest.test.interceptor;

import java.util.function.Supplier;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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
public class ResponseFilterCustomExceptionTest {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
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

    @Before
    public void setup() {
        client = ClientBuilder.newClient();
    }

    @After
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
    public void testThrowCustomException() throws Exception {
        try {
            client.register(ThrowCustomExceptionResponseFilter.class);
            client.target(generateURL("/testCustomException")).request().post(Entity.text("testCustomException"));
        } catch (ProcessingException pe) {
            Assert.assertEquals(CustomException.class, pe.getCause().getClass());
        }
    }
}
