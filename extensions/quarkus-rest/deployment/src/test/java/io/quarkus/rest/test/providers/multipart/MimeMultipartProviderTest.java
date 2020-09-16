package io.quarkus.rest.test.providers.multipart;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.activation.DataHandler;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ProxyBuilder;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.jboss.resteasy.plugins.providers.multipart.MultipartOutput;
import org.jboss.resteasy.plugins.providers.multipart.MultipartRelatedOutput;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.providers.multipart.resource.MimeMultipartProviderClient;
import io.quarkus.rest.test.providers.multipart.resource.MimeMultipartProviderCustomer;
import io.quarkus.rest.test.providers.multipart.resource.MimeMultipartProviderResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Multipart provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Mime Multipart Provider Test")
public class MimeMultipartProviderTest {

    private static Logger logger = Logger.getLogger(MimeMultipartProviderTest.class);

    private static final String TEST_URI = generateURL("/mime");

    static Client client;

    private static final String ERR_NUMBER = "The number of enclosed bodypart objects doesn't match to the expectation";

    @BeforeAll
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void after() throws Exception {
        client.close();
    }

    static final String testFilePath = TestUtil.getResourcePath(MimeMultipartProviderTest.class,
            "HeaderFlushedOutputStreamTestData.txt");

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, MimeMultipartProviderResource.class,
                    MimeMultipartProviderCustomer.class);
        }
    });

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, MimeMultipartProviderTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails MultipartFormDataOutput entity in put request with data from file is used
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Put Form")
    public void testPutForm() throws Exception {
        // prepare file
        File file = new File(testFilePath);
        Assertions.assertTrue("File " + testFilePath + " doesn't exists", file.exists());
        MultipartFormDataOutput mpfdo = new MultipartFormDataOutput();
        mpfdo.addFormData("part1", "This is Value 1", MediaType.TEXT_PLAIN_TYPE);
        mpfdo.addFormData("part2", "This is Value 2", MediaType.TEXT_PLAIN_TYPE);
        mpfdo.addFormData("data.txt", file, MediaType.TEXT_PLAIN_TYPE);
        Response response = client.target(TEST_URI).request().put(Entity.entity(mpfdo, MediaType.MULTIPART_FORM_DATA_TYPE));
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String responseBody = response.readEntity(String.class);
        Assertions.assertEquals(ERR_NUMBER, responseBody, "Count: 3");
    }

    /**
     * @tpTestDetails MultipartOutput entity in put request with data from file is used
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Put")
    public void testPut() throws Exception {
        // prepare file
        File file = new File(testFilePath);
        Assertions.assertTrue("File " + testFilePath + " doesn't exists", file.exists());
        MultipartOutput mpo = new MultipartOutput();
        mpo.addPart("This is Value 1", MediaType.TEXT_PLAIN_TYPE);
        mpo.addPart("This is Value 2", MediaType.TEXT_PLAIN_TYPE);
        mpo.addPart(file, MediaType.TEXT_PLAIN_TYPE);
        Response response = client.target(TEST_URI).request().put(Entity.entity(mpo, MediaType.MULTIPART_FORM_DATA_TYPE));
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String responseBody = response.readEntity(String.class);
        Assertions.assertEquals(ERR_NUMBER, responseBody, "Count: 3");
    }

    /**
     * @tpTestDetails MultipartFormDataOutput entity in put request with created by jxb marshaller is used
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Form")
    public void testForm() throws Exception {
        testMultipart(TEST_URI + "/form");
        testMultipart(TEST_URI + "/form/map");
        testMultipart(TEST_URI + "/form/class");
        testMultipart(TEST_URI + "/multi");
        testMultipart(TEST_URI + "/multi/list");
    }

    private void testMultipart(String uri) throws Exception {
        MultipartFormDataOutput mpfdo = new MultipartFormDataOutput();
        mpfdo.addFormData("bill", createCustomerData("bill"), MediaType.APPLICATION_XML_TYPE);
        mpfdo.addFormData("monica", createCustomerData("monica"), MediaType.APPLICATION_XML_TYPE);
        Response response = client.target(uri).request().put(Entity.entity(mpfdo, MediaType.MULTIPART_FORM_DATA_TYPE));
        Assertions.assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails MultipartOutput entity in put request with manually created jaxb objects is used
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Multipart Output")
    public void testMultipartOutput() throws Exception {
        MimeMultipartProviderClient proxy = ProxyBuilder
                .builder(MimeMultipartProviderClient.class, client.target(generateURL(""))).build();
        MultipartOutput output = new MultipartOutput();
        output.addPart(new MimeMultipartProviderCustomer("bill"), MediaType.APPLICATION_XML_TYPE);
        output.addPart(new MimeMultipartProviderCustomer("monica"), MediaType.APPLICATION_XML_TYPE);
        proxy.putMixed(output);
    }

    /**
     * @tpTestDetails MultipartFormDataOutput entity in put request with manually created jaxb objects is used
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Multipart Form Data Output")
    public void testMultipartFormDataOutput() throws Exception {
        MimeMultipartProviderClient proxy = ProxyBuilder
                .builder(MimeMultipartProviderClient.class, client.target(generateURL(""))).build();
        MultipartFormDataOutput output = new MultipartFormDataOutput();
        output.addFormData("bill", new MimeMultipartProviderCustomer("bill"), MediaType.APPLICATION_XML_TYPE);
        output.addFormData("monica", new MimeMultipartProviderCustomer("monica"), MediaType.APPLICATION_XML_TYPE);
        proxy.putFormData(output);
    }

    /**
     * @tpTestDetails MultipartRelatedOutput entity in put request with manually generated xml is used
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Multipart Related Output")
    public void testMultipartRelatedOutput() throws Exception {
        MimeMultipartProviderClient proxy = ProxyBuilder
                .builder(MimeMultipartProviderClient.class, client.target(generateURL(""))).build();
        MultipartRelatedOutput output = new MultipartRelatedOutput();
        output.setStartInfo("text/xml");
        Map<String, String> mediaTypeParameters = new LinkedHashMap<String, String>();
        mediaTypeParameters.put("charset", StandardCharsets.UTF_8.name());
        mediaTypeParameters.put("type", "text/xml");
        output.addPart("<m:data xmlns:m='http://example.org/stuff'>"
                + "<m:photo><xop:Include xmlns:xop='http://www.w3.org/2004/08/xop/include' href='cid:http://example.org/me.png'/></m:photo>"
                + "<m:sig><xop:Include xmlns:xop='http://www.w3.org/2004/08/xop/include' href='cid:http://example.org/my.hsh'/></m:sig>"
                + "</m:data>", new MediaType("application", "xop+xml", mediaTypeParameters), "<mymessage.xml@example.org>",
                "8bit");
        output.addPart("// binary octets for png", new MediaType("image", "png"), "<http://example.org/me.png>", "binary");
        output.addPart("// binary octets for signature", new MediaType("application", "pkcs7-signature"),
                "<http://example.org/me.hsh>", "binary");
        proxy.putRelated(output);
    }

    /**
     * @tpTestDetails List is send in put request with the @PartType annotation
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Multipart List")
    public void testMultipartList() throws Exception {
        MimeMultipartProviderClient proxy = ProxyBuilder
                .builder(MimeMultipartProviderClient.class, client.target(generateURL(""))).build();
        ArrayList<MimeMultipartProviderCustomer> mimeMultipartProviderCustomers = new ArrayList<MimeMultipartProviderCustomer>();
        mimeMultipartProviderCustomers.add(new MimeMultipartProviderCustomer("bill"));
        mimeMultipartProviderCustomers.add(new MimeMultipartProviderCustomer("monica"));
        proxy.putMixedList(mimeMultipartProviderCustomers);
    }

    /**
     * @tpTestDetails Map is send in put request with the @PartType annotation
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Multipart Map")
    public void testMultipartMap() throws Exception {
        MimeMultipartProviderClient proxy = ProxyBuilder
                .builder(MimeMultipartProviderClient.class, client.target(generateURL(""))).build();
        LinkedHashMap<String, MimeMultipartProviderCustomer> customers = new LinkedHashMap<String, MimeMultipartProviderCustomer>();
        customers.put("bill", new MimeMultipartProviderCustomer("bill"));
        customers.put("monica", new MimeMultipartProviderCustomer("monica"));
        proxy.putFormDataMap(customers);
    }

    /**
     * @tpTestDetails Custom Form type in put request with @MultipartForm annotation
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Multipart Form")
    public void testMultipartForm() throws Exception {
        MimeMultipartProviderClient proxy = ProxyBuilder
                .builder(MimeMultipartProviderClient.class, client.target(generateURL(""))).build();
        MimeMultipartProviderResource.Form form = new MimeMultipartProviderResource.Form(
                new MimeMultipartProviderCustomer("bill"), new MimeMultipartProviderCustomer("monica"));
        proxy.putFormDataMap(form);
    }

    /**
     * @tpTestDetails Custom jaxb object in put request with @XopWithMultipartRelated
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Xop")
    public void testXop() throws Exception {
        MimeMultipartProviderClient proxy = ProxyBuilder
                .builder(MimeMultipartProviderClient.class, client.target(generateURL(""))).build();
        MimeMultipartProviderResource.Xop xop = new MimeMultipartProviderResource.Xop(
                new MimeMultipartProviderCustomer("bill\u00E9"), new MimeMultipartProviderCustomer("monica"),
                "Hello Xop World!".getBytes(StandardCharsets.UTF_8),
                new DataHandler(new ByteArrayDataSource("Hello Xop World!".getBytes(StandardCharsets.UTF_8),
                        MediaType.APPLICATION_OCTET_STREAM)));
        proxy.putXop(xop);
    }

    private String createCustomerData(String name) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(MimeMultipartProviderCustomer.class);
        StringWriter writer = new StringWriter();
        context.createMarshaller().marshal(new MimeMultipartProviderCustomer(name), writer);
        String data = writer.toString();
        return data;
    }

    /**
     * @tpTestDetails Client sends get request for InputStream from the server
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get")
    public void testGet() throws Exception {
        Response response = client.target(TEST_URI).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        BufferedInputStream in = new BufferedInputStream(response.readEntity(InputStream.class));
        String contentType = response.getStringHeaders().getFirst("content-type");
        ByteArrayDataSource ds = new ByteArrayDataSource(in, contentType);
        MimeMultipart mimeMultipart = new MimeMultipart(ds);
        Assertions.assertEquals(ERR_NUMBER, mimeMultipart.getCount(), 2);
        response.close();
    }

    /**
     * @tpTestDetails Client sends post request with "multipart/form-data" and boundary definition
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test File")
    public void testFile() throws Exception {
        Response response = client.target(TEST_URI + "/file/test").request().post(
                Entity.entity(form, "multipart/form-data; boundary=---------------------------52524491016334132001492192799"));
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
    }

    private static final String form = "-----------------------------52524491016334132001492192799\r\n"
            + "Content-Disposition: form-data; name=\"submit-name\"\r\n" + "\r\n" + "Bill\r\n"
            + "-----------------------------52524491016334132001492192799\r\n"
            + "Content-Disposition: form-data; name=\"files\"; filename=\"stuff.txt\"\r\n" + "Content-Type: text/plain\r\n"
            + "\r\n" + "hello world\r\n" + "\r\n" + "-----------------------------52524491016334132001492192799--";
}
