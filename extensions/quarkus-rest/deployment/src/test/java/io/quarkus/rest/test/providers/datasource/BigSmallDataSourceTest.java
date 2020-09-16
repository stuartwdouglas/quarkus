package io.quarkus.rest.test.providers.datasource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
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
import io.quarkus.rest.test.providers.datasource.resource.BigSmallDataSourceResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter DataSource provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Big Small Data Source Test")
public class BigSmallDataSourceTest {

    static QuarkusRestClient client;

    static final String testFilePath;

    static {
        testFilePath = TestUtil.getResourcePath(BigSmallDataSourceTest.class, "test.jpg");
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, BigSmallDataSourceResource.class);
        }
    });

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, BigSmallDataSourceTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Tests DataSourceProviders ability to get content type of the file attached to the request
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Post Data Source")
    public void testPostDataSource() throws Exception {
        File file = new File(testFilePath);
        Assertions.assertTrue("File " + testFilePath + " doesn't exists", file.exists());
        WebTarget target = client.target(generateURL("/jaf"));
        Response response = target.request().post(Entity.entity(file, "image/jpeg"));
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("image/jpeg", response.readEntity(String.class),
                "Unexpected content type returned from the server");
    }

    /**
     * @tpTestDetails Tests DataSourceProviders ability to read and write bigger file
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Echo Data Source Big Data")
    public void testEchoDataSourceBigData() throws Exception {
        WebTarget target = client.target(generateURL("/jaf/echo"));
        File file = new File(testFilePath);
        Assertions.assertTrue("File " + testFilePath + " doesn't exists", file.exists());
        Response response = target.request().post(Entity.entity(file, "image/jpeg"));
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        InputStream ris = null;
        InputStream fis = null;
        try {
            ris = response.readEntity(InputStream.class);
            fis = new FileInputStream(file);
            int fi;
            int ri;
            do {
                fi = fis.read();
                ri = ris.read();
                if (fi != ri) {
                    Assertions.fail("The sent and received stream is not identical.");
                }
            } while (fi != -1);
        } finally {
            if (ris != null) {
                ris.close();
            }
            if (fis != null) {
                fis.close();
            }
        }
    }

    /**
     * @tpTestDetails Tests DataSourceProviders ability to read and write small stream
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Echo Data Source Small Data")
    public void testEchoDataSourceSmallData() throws Exception {
        WebTarget target = client.target(generateURL("/jaf/echo"));
        byte[] input = "Hello World!".getBytes(StandardCharsets.UTF_8);
        Response response = target.request().post(Entity.entity(input, MediaType.APPLICATION_OCTET_STREAM));
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        InputStream ris = null;
        InputStream bis = null;
        try {
            ris = response.readEntity(InputStream.class);
            bis = new ByteArrayInputStream(input);
            int fi;
            int ri;
            do {
                fi = bis.read();
                ri = ris.read();
                if (fi != ri) {
                    Assertions.fail("The sent and recived stream is not identical.");
                }
            } while (fi != -1);
        } finally {
            if (ris != null) {
                ris.close();
            }
            if (bis != null) {
                bis.close();
            }
        }
    }

    /**
     * @tpTestDetails Tests DataSourceProviders ability to return InputStream for given value
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Data Source")
    public void testGetDataSource() throws Exception {
        String value = "foo";
        WebTarget target = client.target(generateURL("/jaf") + "/" + value);
        Response response = target.request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(value, response.readEntity(String.class), "The unexpected value returned from InputStream");
    }
}
