package io.quarkus.rest.test.providers.multipart;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
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
public class InputPartDefaultContentTypeEncodingOverwriteTest {
    public static final String TEXT_PLAIN_WITH_CHARSET_UTF_8 = "text/plain; charset=utf-8";
    private static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
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

    @BeforeClass
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
    }

    @AfterClass
    public static void after() throws Exception {
        client.close();
    }

    private static final String TEST_URI = generateURL("");

    /**
     * @tpTestDetails Test for new client
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testContentTypeNewClient() throws Exception {
        String message = "--boo\r\n"
                + "Content-Disposition: form-data; name=\"foo\"\r\n"
                + "Content-Transfer-Encoding: 8bit\r\n\r\n" + "bar\r\n"
                + "--boo--\r\n";

        WebTarget target = client.target(generateURL("/mime"));
        Entity entity = Entity.entity(message, "multipart/form-data; boundary=boo");
        Response response = target.request().post(entity);

        Assert.assertEquals("Status code is wrong.", 20, response.getStatus() / 10);
        Assert.assertEquals("Response text is wrong",
                MediaType.valueOf(TEXT_PLAIN_WITH_CHARSET_UTF_8),
                MediaType.valueOf(response.readEntity(String.class)));
    }
}
