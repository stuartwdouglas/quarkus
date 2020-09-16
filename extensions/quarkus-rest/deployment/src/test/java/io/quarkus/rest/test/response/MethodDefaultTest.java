package io.quarkus.rest.test.response;

import java.util.HashSet;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.response.resource.MethodDefaultResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Response
 * @tpChapter Integration tests
 * @tpTestCaseDetails Spec requires that HEAD and OPTIONS are handled in a default manner
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Method Default Test")
public class MethodDefaultTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, MethodDefaultResource.class);
        }
    });

    @BeforeAll
    public static void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterAll
    public static void close() {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, MethodDefaultTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Client invokes Head on root resource at /GetTest;
     *                which no request method designated for HEAD;
     *                Verify that corresponding GET Method is invoked.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Head Plain")
    public void testHeadPlain() throws Exception {
        Response response = client.target(generateURL("/GetTest")).request().header("Accept", "text/plain").head();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String header = response.getHeaderString("CTS-HEAD");
        Assertions.assertEquals("text-plain", header, "Wrong CTS-HEAD header");
        response.close();
    }

    /**
     * @tpTestDetails Client invokes HEAD on root resource at /GetTest;
     *                which no request method designated for HEAD;
     *                Verify that corresponding GET Method is invoked.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Head Html")
    public void testHeadHtml() throws Exception {
        Response response = client.target(generateURL("/GetTest")).request().header("Accept", "text/html").head();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String header = response.getHeaderString("CTS-HEAD");
        Assertions.assertEquals("text-html", header, "Wrong CTS-HEAD header");
        response.close();
    }

    /**
     * @tpTestDetails Client invokes HEAD on sub resource at /GetTest/sub;
     *                which no request method designated for HEAD;
     *                Verify that corresponding GET Method is invoked instead.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Head Subresource")
    public void testHeadSubresource() throws Exception {
        Response response = client.target(generateURL("/GetTest/sub")).request().header("Accept", "text/plain").head();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String header = response.getHeaderString("CTS-HEAD");
        Assertions.assertEquals("sub-text-plain", header, "Wrong CTS-HEAD header");
        response.close();
    }

    /**
     * @tpTestDetails If client invokes OPTIONS and there is no request method that exists, verify that an automatic response is
     *                generated
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Options")
    public void testOptions() throws Exception {
        Response response = client.target(generateURL("/GetTest/sub")).request().options();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String allowedHeader = response.getHeaderString("Allow");
        Assertions.assertNotNull(allowedHeader, "Wrong Allow header");
        String[] allowed = allowedHeader.split(",");
        HashSet<String> set = new HashSet<String>();
        for (String allow : allowed) {
            set.add(allow.trim());
        }
        Assertions.assertTrue(set.contains("GET"), "Wrong Allow header");
        Assertions.assertTrue(set.contains("OPTIONS"), "Wrong Allow header");
        Assertions.assertTrue(set.contains("HEAD"), "Wrong Allow header");
        response.close();
    }
}
