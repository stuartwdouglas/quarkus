package io.quarkus.rest.test.cdi.interceptors;

import static org.junit.Assert.assertEquals;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for interceptors with timer service.
 * @tpSince RESTEasy 3.0.16
 */
public class TimerInterceptorTest {
    protected static final Logger log = LogManager.getLogger(TimerInterceptorTest.class.getName());

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return war;
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, TimerInterceptorTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Timer is sheduled and than is called.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testTimerInterceptor() throws Exception {
        Client client = ClientBuilder.newClient();

        // Schedule timer.
        WebTarget base = client.target(generateURL("/timer/schedule"));
        Response response = base.request().get();
        log.info("Status: " + response.getStatus());
        assertEquals(200, response.getStatus());
        response.close();

        // Verify timer expired and timer interceptor was executed.
        base = client.target(generateURL("/timer/test"));
        response = base.request().get();
        log.info("Status: " + response.getStatus());
        assertEquals(200, response.getStatus());
        response.close();

        client.close();
    }
}
