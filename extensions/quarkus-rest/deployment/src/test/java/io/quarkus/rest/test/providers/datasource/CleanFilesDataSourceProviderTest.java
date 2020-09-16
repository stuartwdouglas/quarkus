package io.quarkus.rest.test.providers.datasource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.datasource.resource.CleanFilesDataSourceProviderResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter DataSource provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Clean Files Data Source Provider Test")
public class CleanFilesDataSourceProviderTest {

    protected static final Logger logger = Logger.getLogger(CleanFilesDataSourceProviderTest.class.getName());

    static QuarkusRestClient client;

    static String serverTmpDir;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            // DataSource provider creates tmp file in the filesystem
            return TestUtil.finishContainerPrepare(war, null, CleanFilesDataSourceProviderResource.class);
        }
    });

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
        serverTmpDir = getTmpDirectory();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, CleanFilesDataSourceProviderTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Tests DataSourceProviders ability to read the same stream and then checks whether number of temporary
     *                files is same as before request. The manipulation with DataSourceProvider happens on the server, no data
     *                are send
     *                back and forth
     * @tpInfo RESTEASY-1182
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Data Source Provider Input Stream Once")
    public void testDataSourceProviderInputStreamOnce() throws Exception {
        // count temporary files before
        int countBefore = countFiles(serverTmpDir);
        logger.info("Count of Resteasy temporary files in " + serverTmpDir + " before request: " + countBefore);
        // http request
        HttpClient httpClient = HttpClients.custom().build();
        HttpPost httpPost = new HttpPost(generateURL("/once"));
        httpPost.setHeader("Content-type", "application/octet-stream");
        httpPost.setEntity(new ByteArrayEntity(new byte[5 * 1024]));
        HttpResponse response = httpClient.execute(httpPost);
        // check http request results
        int postStatus = response.getStatusLine().getStatusCode();
        String postResponse = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        Assertions.assertEquals(HttpStatus.SC_OK.getStatusCode(), postStatus, "Status of client request is not correct.");
        Assertions.assertEquals(CleanFilesDataSourceProviderResource.clientResponse, postResponse,
                "Client get wrong response.");
        // count temporary files after
        int countAfter = countFiles(serverTmpDir);
        logger.info("Count of Resteasy temporary files in " + serverTmpDir + " after request: " + countAfter);
        // Compare
        Assertions.assertEquals(countBefore, countAfter, "Client request remove or add some temporary files.");
    }

    /**
     * @tpTestDetails Tests DataSourceProviders ability to read the same stream twice and then checks whether number of
     *                temporary
     *                files is same as before request. The manipulation with DataSourceProvider happens on the server, no data
     *                are send
     *                back and forth
     * @tpInfo RESTEASY-1182
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Data Source Provider Input Stream Twice")
    public void testDataSourceProviderInputStreamTwice() throws Exception {
        // count temporary files before
        int countBefore = countFiles(serverTmpDir);
        logger.info("Count of Resteasy temporary files in " + serverTmpDir + " before request: " + countBefore);
        // http request
        HttpClient httpClient = HttpClients.custom().build();
        HttpPost httpPost = new HttpPost(generateURL("/twice"));
        httpPost.setHeader("Content-type", "application/octet-stream");
        httpPost.setEntity(new ByteArrayEntity(new byte[5 * 1024]));
        HttpResponse response = httpClient.execute(httpPost);
        // check http request results
        int postStatus = response.getStatusLine().getStatusCode();
        String postResponse = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        Assertions.assertEquals(TestUtil.getErrorMessageForKnownIssue("JBEAP-1904", "Status of client request is not correct."),
                HttpStatus.SC_OK, postStatus);
        Assertions.assertEquals(CleanFilesDataSourceProviderResource.clientResponse, postResponse,
                "Client get wrong response.");
        // count temporary files after
        int countAfter = countFiles(serverTmpDir);
        logger.info("Count of Resteasy temporary files in " + serverTmpDir + " after request: " + countAfter);
        // Compare
        Assertions.assertEquals(countBefore, countAfter, "Client request remove or add some temporary files.");
    }

    /**
     * @tpTestDetails Tests that DataSourceProvider removes temporary file it creates in the case when input stream is not read.
     * @tpInfo RESTEASY-1670
     * @tpSince RESTEasy 3.0.24
     */
    @Test
    @DisplayName("Test Data Source Provider Input Stream Not Read")
    public void testDataSourceProviderInputStreamNotRead() throws Exception {
        // count temporary files before
        int countBefore = countFiles(serverTmpDir);
        logger.info("Count of Resteasy temporary files in " + serverTmpDir + " before request: " + countBefore);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(5000);
        for (int i = 0; i < 5000; i++) {
            baos.write(i);
        }
        Response response = client.target(generateURL("/never")).request()
                .post(Entity.entity(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // count temporary files after
        int countAfter = countFiles(serverTmpDir);
        logger.info("Count of Resteasy temporary files in " + serverTmpDir + " after request: " + countAfter);
        // Compare
        Assertions.assertEquals(countBefore, countAfter, "Client request removed or added some temporary files.");
    }

    private static String getTmpDirectory() {
        Response response = client.target(generateURL("/tmpdirpath")).request().get();
        return response.readEntity(String.class);
    }

    private int countFiles(String dir) {
        File tmpdir = new File(dir);
        Assertions.assertTrue(dir + " does not exists", tmpdir.isDirectory());
        logger.info("Tmp directory = " + tmpdir);
        // Get count of Resteasy temporary files
        String[] tmpfiles = tmpdir.list(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.startsWith("resteasy-provider-datasource");
            }
        });
        return tmpfiles.length;
    }
}
