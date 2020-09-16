package io.quarkus.rest.test.core.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.core.basic.resource.InvalidMediaTypeResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpSince RESTEasy 3.0.16
 * @tpTestCaseDetails Regression for RESTEASY-699
 */
@DisplayName("Invalid Media Type Test")
public class InvalidMediaTypeTest {

    protected static final Logger logger = Logger.getLogger(InvalidMediaTypeTest.class.getName());

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, InvalidMediaTypeResource.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, InvalidMediaTypeTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Check various wrong media type
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Invalid Media Types")
    public void testInvalidMediaTypes() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        Invocation.Builder request = client.target(generateURL("/test")).request();
        // Missing type or subtype
        doTest(request, "/");
        doTest(request, "/*");
        doTest(request, "*/");
        doTest(request, "text/");
        doTest(request, "/plain");
        // Illegal white space
        doTest(request, " /*");
        doTest(request, "/* ");
        doTest(request, " /* ");
        doTest(request, "/ *");
        doTest(request, "* /");
        doTest(request, " / *");
        doTest(request, "* / ");
        doTest(request, "* / *");
        doTest(request, " * / *");
        doTest(request, "* / * ");
        doTest(request, "text/ plain");
        doTest(request, "text /plain");
        doTest(request, " text/plain");
        doTest(request, "text/plain ");
        doTest(request, " text/plain ");
        doTest(request, " text / plain ");
        client.close();
    }

    private void doTest(Invocation.Builder request, String mediaType) {
        request.accept(mediaType);
        Response response = request.get();
        logger.info("mediaType: " + mediaType + "");
        logger.info("status: " + response.getStatus());
        Assertions.assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        response.close();
    }
}
