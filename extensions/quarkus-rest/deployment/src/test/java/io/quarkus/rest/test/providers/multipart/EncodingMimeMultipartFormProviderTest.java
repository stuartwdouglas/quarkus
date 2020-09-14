package io.quarkus.rest.test.providers.multipart;

import java.io.File;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.multipart.resource.EncodingMimeMultipartFormProviderResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Multipart provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.6.0
 */
public class EncodingMimeMultipartFormProviderTest {

    private static final String TEST_URI = generateURL("/encoding-mime");
    // file with non ASCII character
    private static final String testFilePath = TestUtil.getResourcePath(EncodingMimeMultipartFormProviderTest.class,
            "EncodingMimeMultipartFormProviderTestData.txt");

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, EncodingMimeMultipartFormProviderResource.class);
                }
            });

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, EncodingMimeMultipartFormProviderTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test of filename encoding
     * @tpSince RESTEasy 3.6.0
     */
    @Test
    public void testPostFormFile() throws Exception {
        // prepare file
        File file = new File(testFilePath);
        Assert.assertTrue("File " + testFilePath + " doesn't exists", file.exists());

        MultipartFormDataOutput mpfdo = new MultipartFormDataOutput();
        mpfdo.addFormData("file_upload", file, MediaType.APPLICATION_OCTET_STREAM_TYPE,
                EncodingMimeMultipartFormProviderResource.FILENAME_NON_ASCII, true);

        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        Response response = client.target(TEST_URI + "/file").request()
                .post(Entity.entity(mpfdo, MediaType.MULTIPART_FORM_DATA_TYPE));
        Assert.assertEquals(Status.NO_CONTENT, response.getStatus());
        client.close();
    }
}
