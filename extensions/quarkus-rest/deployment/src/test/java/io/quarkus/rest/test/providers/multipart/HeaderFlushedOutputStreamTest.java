package io.quarkus.rest.test.providers.multipart;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.function.Supplier;

import javax.mail.BodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.jboss.resteasy.plugins.providers.multipart.MultipartOutput;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.providers.multipart.resource.HeaderFlushedOutputStreamBean;
import io.quarkus.rest.test.providers.multipart.resource.HeaderFlushedOutputStreamService;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Multipart provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-190
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Header Flushed Output Stream Test")
public class HeaderFlushedOutputStreamTest {

    static Client client;

    @BeforeAll
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void after() throws Exception {
        client.close();
    }

    static final String testFilePath;

    static {
        testFilePath = TestUtil.getResourcePath(HeaderFlushedOutputStreamTest.class, "HeaderFlushedOutputStreamTestData.txt");
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(HeaderFlushedOutputStreamBean.class);
            return TestUtil.finishContainerPrepare(war, null, HeaderFlushedOutputStreamService.class);
        }
    });

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, HeaderFlushedOutputStreamTest.class.getSimpleName());
    }

    private static final String TEST_URI = generateURL("/mime");

    /**
     * @tpTestDetails Loopback to examine form-data
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Post")
    public void testPost() throws Exception {
        // prepare file
        File file = new File(testFilePath);
        Assertions.assertTrue("File " + testFilePath + " doesn't exists", file.exists());
        // test logic
        MultipartOutput mpo = new MultipartOutput();
        mpo.addPart("This is Value 1", MediaType.TEXT_PLAIN_TYPE);
        mpo.addPart("This is Value 2", MediaType.TEXT_PLAIN_TYPE);
        mpo.addPart(file, MediaType.TEXT_PLAIN_TYPE);
        Response response = client.target(TEST_URI).request().post(Entity.entity(mpo, MediaType.MULTIPART_FORM_DATA_TYPE));
        BufferedInputStream in = new BufferedInputStream(response.readEntity(InputStream.class));
        String contentType = response.getHeaderString("content-type");
        ByteArrayDataSource ds = new ByteArrayDataSource(in, contentType);
        MimeMultipart mimeMultipart = new MimeMultipart(ds);
        Assertions.assertEquals(mimeMultipart.getCount(), 3, "Wrong count of parts of response");
        response.close();
    }

    /**
     * @tpTestDetails Test post method
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Post Form")
    public void testPostForm() throws Exception {
        // prepare file
        File file = new File(testFilePath);
        Assertions.assertTrue("File " + testFilePath + " doesn't exists", file.exists());
        // test logic
        MultipartFormDataOutput mpfdo = new MultipartFormDataOutput();
        mpfdo.addFormData("part1", "This is Value 1", MediaType.TEXT_PLAIN_TYPE);
        mpfdo.addFormData("part2", "This is Value 2", MediaType.TEXT_PLAIN_TYPE);
        mpfdo.addFormData("data.txt", file, MediaType.TEXT_PLAIN_TYPE);
        Response response = client.target(TEST_URI).request().post(Entity.entity(mpfdo, MediaType.MULTIPART_FORM_DATA_TYPE));
        BufferedInputStream in = new BufferedInputStream(response.readEntity(InputStream.class));
        String contentType = response.getHeaderString("content-type");
        ByteArrayDataSource ds = new ByteArrayDataSource(in, contentType);
        MimeMultipart mimeMultipart = new MimeMultipart(ds);
        Assertions.assertEquals(mimeMultipart.getCount(), 3, "Wrong count of parts of response");
        response.close();
    }

    /**
     * @tpTestDetails Test get method
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get")
    public void testGet() throws Exception {
        Response response = client.target(TEST_URI).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        BufferedInputStream in = new BufferedInputStream(response.readEntity(InputStream.class));
        String contentType = response.getHeaderString("content-type");
        ByteArrayDataSource ds = new ByteArrayDataSource(in, contentType);
        MimeMultipart mimeMultipart = new MimeMultipart(ds);
        Assertions.assertEquals(mimeMultipart.getCount(), 1, "Wrong count of parts of response");
        BodyPart part = mimeMultipart.getBodyPart(0);
        InputStream is = part.getInputStream();
        Assertions.assertEquals(3, part.getSize(), "Wrong count of parts of response");
        char[] output = new char[3];
        output[0] = (char) is.read();
        output[1] = (char) is.read();
        output[2] = (char) is.read();
        String str = new String(output);
        Assertions.assertEquals("bla", str, "Wrong content of first part of response");
    }
}
