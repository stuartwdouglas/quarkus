package io.quarkus.rest.test.providers.iioimage;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.iioimage.resource.ImageResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter IIOImage provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Basic test for IIOImage provider. Old issue: https://issues.jboss.org/browse/RESTEASY-862
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Iio Image Provider Test")
public class IIOImageProviderTest {

    static QuarkusRestClient client;

    // two different versions of the same png image, compressed using JDK8 and JDK11, so that we can perform byte comparisons in testPostPNGImage()
    static final String testPngResource1 = "test1.png";

    static final String testPngResource2 = "test2.png";

    static final String testWdpResource = "test.wdp";

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    private static final String TEST_URI = generateURL("/image");

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, ImageResource.class);
        }
    });

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, IIOImageProviderTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test a post of a PNG image whose response should be a PNG version of the
     *                same photo.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Post PNG Image")
    public void testPostPNGImage() throws Exception {
        final String testPngResource = System.getProperty("java.version").startsWith("1.") ? testPngResource1
                : testPngResource2;
        File file = new File(TestUtil.getResourcePath(IIOImageProviderTest.class, testPngResource));
        Assertions.assertTrue(file.exists());
        Response response = client.target(TEST_URI).request().post(Entity.entity(file, "image/png"));
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String contentType = response.getHeaderString("content-type");
        Assertions.assertEquals("image/png", contentType, "Wrong content type of response");
        BufferedInputStream in = new BufferedInputStream(response.readEntity(InputStream.class));
        ByteArrayOutputStream fromServer = new ByteArrayOutputStream();
        writeTo(in, fromServer);
        response.close();
        File savedPng = new File(TestUtil.getResourcePath(IIOImageProviderTest.class, testPngResource));
        FileInputStream fis = new FileInputStream(savedPng);
        ByteArrayOutputStream fromTestData = new ByteArrayOutputStream();
        writeTo(fis, fromTestData);
        // ImageResource could change image slightly, so next assert could fail, because same picture could have been saved different
        Assertions.assertTrue(Arrays.equals(fromServer.toByteArray(), fromTestData.toByteArray()),
                "ImageResource could change image slightly or ImageResource is wrong");
    }

    /**
     * @tpTestDetails Tests a image format that is not directly supported by Image IO. In this
     *                case, an HD Photo image is posted to the Resource which should return a
     *                406 - Not Acceptable response. The response body should include a list of
     *                variants that are supported by the application.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Post Unsupported Image")
    public void testPostUnsupportedImage() throws Exception {
        File file = new File(TestUtil.getResourcePath(IIOImageProviderTest.class, testWdpResource));
        Assertions.assertTrue(file.exists());
        Response response = client.target(TEST_URI).request().post(Entity.entity(file, "image/vnd.ms-photo"));
        Assertions.assertEquals(Status.NOT_ACCEPTABLE.getStatusCode(), response.getStatus(),
                "Unsupported image is accepted by server");
        response.close();
    }

    public void writeTo(final InputStream in, final OutputStream out) throws IOException {
        int read;
        final byte[] buf = new byte[2048];
        while ((read = in.read(buf)) != -1) {
            out.write(buf, 0, read);
        }
    }
}
