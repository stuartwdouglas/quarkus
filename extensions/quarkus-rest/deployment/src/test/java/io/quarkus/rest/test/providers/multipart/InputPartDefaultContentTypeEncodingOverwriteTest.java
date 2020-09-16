package io.quarkus.rest.test.providers.multipart;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.providers.multipart.resource.InputPartDefaultContentTypeEncodingOverwriteService;
import io.quarkus.rest.test.providers.multipart.resource.InputPartDefaultContentTypeEncodingOverwriteSetterContainerRequestFilter;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Multipart provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for default content type encoding of multipart provider
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Input Part Default Content Type Encoding Overwrite Test")
public class InputPartDefaultContentTypeEncodingOverwriteTest {

    public static final String TEXT_PLAIN_WITH_CHARSET_UTF_8 = "text/plain; charset=utf-8";

    private static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(InputPartDefaultContentTypeEncodingOverwriteTest.class);
            war.addClasses(TestUtil.class, PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null,
                    InputPartDefaultContentTypeEncodingOverwriteSetterContainerRequestFilter.class,
                    InputPartDefaultContentTypeEncodingOverwriteService.class);
        }
    });

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, InputPartDefaultContentTypeEncodingOverwriteTest.class.getSimpleName());
    }

    @BeforeAll
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void after() throws Exception {
        client.close();
    }

    private static final String TEST_URI = generateURL("");

    /**
     * @tpTestDetails Test for new client
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Content Type New Client")
    public void testContentTypeNewClient() throws Exception {
        String message = "--boo\r\n" + "Content-Disposition: form-data; name=\"foo\"\r\n"
                + "Content-Transfer-Encoding: 8bit\r\n\r\n" + "bar\r\n" + "--boo--\r\n";
        WebTarget target = client.target(generateURL("/mime"));
        Entity entity = Entity.entity(message, "multipart/form-data; boundary=boo");
        Response response = target.request().post(entity);
        Assertions.assertEquals(20, response.getStatus() / 10, "Status code is wrong.");
        Assertions.assertEquals(MediaType.valueOf(TEXT_PLAIN_WITH_CHARSET_UTF_8),
                MediaType.valueOf(response.readEntity(String.class)), "Response text is wrong");
    }
}
