package io.quarkus.rest.test.core.encoding;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.core.encoding.resource.EncodedParamsComplexResource;
import io.quarkus.rest.test.core.encoding.resource.EncodedParamsSimpleResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Encoding
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for special characters in get request
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Encoded Params Test")
public class EncodedParamsTest {

    public static final String ERROR_MESSAGE = "Wrong encoded characters in request";

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(EncodedParamsTest.class);
            return TestUtil.finishContainerPrepare(war, null, EncodedParamsComplexResource.class,
                    EncodedParamsSimpleResource.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, EncodedParamsTest.class.getSimpleName());
    }

    private void basicTest(String path) {
        Client client = ClientBuilder.newClient();
        Response response = client.target(generateURL(path)).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
        client.close();
    }

    /**
     * @tpTestDetails Check various location of "?", "%20" characters
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Encoded")
    public void testEncoded() throws Exception {
        basicTest("/encodedParam?hello%20world=5&stuff=hello%20world");
        basicTest("/encodedParam/hello%20world");
        basicTest("/encodedMethod?hello%20world=5&stuff=hello%20world");
        basicTest("/encodedMethod/hello%20world");
    }
}
