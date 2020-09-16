package io.quarkus.rest.test.exception;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.client.Client;

import org.jboss.resteasy.client.jaxrs.internal.QuarkusRestClientBuilderImpl;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.exception.resource.ClosedResponseHandlingEnableTracingRequestFilter;
import io.quarkus.rest.test.exception.resource.ClosedResponseHandlingPleaseMapExceptionMapper;
import io.quarkus.rest.test.exception.resource.ClosedResponseHandlingResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 4.0.0.CR1
 * @tpTestCaseDetails Regression test for RESTEASY-1142
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @author <a href="jonas.zeiger@talpidae.net">Jonas Zeiger</a>
 */
@DisplayName("Closed Response Handling Test")
public class ClosedResponseHandlingTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(ClosedResponseHandlingTest.class);
            war.addPackage(ClosedResponseHandlingResource.class.getPackage());
            war.addClass(PortProviderUtil.class);
            Map<String, String> params = new HashMap<>();
            params.put(ResteasyContextParameters.RESTEASY_TRACING_TYPE, ResteasyContextParameters.RESTEASY_TRACING_TYPE_ALL);
            params.put(ResteasyContextParameters.RESTEASY_TRACING_THRESHOLD,
                    ResteasyContextParameters.RESTEASY_TRACING_LEVEL_VERBOSE);
            return TestUtil.finishContainerPrepare(war, params, ClosedResponseHandlingResource.class,
                    ClosedResponseHandlingPleaseMapExceptionMapper.class,
                    ClosedResponseHandlingEnableTracingRequestFilter.class);
        }
    });

    /**
     * @tpTestDetails RESTEasy client errors that result in a closed Response are correctly handled.
     * @tpPassCrit A NotAcceptableException is returned
     * @tpSince RESTEasy 4.0.0.CR1
     */
    @Test
    @DisplayName("Test Not Acceptable")
    public void testNotAcceptable() {
        assertThrows(NotAcceptableException.class, () -> {
            Client c = new QuarkusRestClientBuilderImpl().build();
            try {
                c.target(generateURL("/testNotAcceptable")).request().get(String.class);
            } finally {
                c.close();
            }
        });
    }

    /**
     * @tpTestDetails Closed Response instances should be handled correctly with full tracing enabled.
     * @tpPassCrit A NotSupportedException is returned
     * @tpSince RESTEasy 4.0.0.CR1
     */
    @Test
    @DisplayName("Test Not Supported Traced")
    public void testNotSupportedTraced() {
        assertThrows(NotSupportedException.class, () -> {
            Client c = new QuarkusRestClientBuilderImpl().build();
            try {
                c.target(generateURL("/testNotSupportedTraced")).request().get(String.class);
            } finally {
                c.close();
            }
        });
    }

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ClosedResponseHandlingTest.class.getSimpleName());
    }
}
