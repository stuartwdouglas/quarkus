package io.quarkus.rest.test.providers.datasource;

import java.io.ByteArrayOutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.providers.datasource.resource.ReadDataSourceTwiceCountTempFileResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter DataSource provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class ReadDataSourceTwiceCountTempFileTest {

    protected static final Logger logger = Logger.getLogger(ReadDataSourceTwiceCountTempFileResource.class.getName());

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    // DataSource provider creates tmp file in the filesystem

                    return TestUtil.finishContainerPrepare(war, null, ReadDataSourceTwiceCountTempFileResource.class);
                }
            });

    @Before
    public void init() {
        client = ClientBuilder.newClient();
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ReadDataSourceTwiceCountTempFileResource.class.getSimpleName());
    }

    /**
     * @tpTestDetails Tests DataSourceProviders ability to read the same stream twice, consuming content of whole stream
     *                before reading the second and verifies that no temporary file left after stream is closed
     * @tpInfo RESTEASY-1182
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testFileNotFound() throws Exception {
        WebTarget target = client.target(generateURL("/post"));

        //Count files initially
        int beginning = countTempFiles();

        ByteArrayOutputStream baos = new ByteArrayOutputStream(5000);
        for (int i = 0; i < 5000; i++) {
            baos.write(i);
        }
        Response response = target.request().post(Entity.entity(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));
        logger.info("The status of the response is " + response.getStatus());
        Assert.assertEquals(TestUtil.getErrorMessageForKnownIssue("JBEAP-2847"), Status.OK, response.getStatus());
        int counter = response.readEntity(int.class);
        int updated = countTempFiles();
        logger.info("counter from beginning (before request): " + beginning);
        logger.info("counter from server: " + counter);
        logger.info("counter updated: " + countTempFiles());
        Assert.assertTrue("The number of temporary files for datasource before and after request is not the same",
                counter > updated);
    }

    /**
     * @tpTestDetails Tests DataSourceProviders ability to read the same stream twice, consuming content of whole stream
     *                before reading the second and verifies that no temporary file left after stream is closed. The request is
     *                send multiple
     *                times and then number of files is verified
     * @tpInfo RESTEASY-1182
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testFileNotFoundMultipleRequests() throws Exception {
        WebTarget target = client.target(generateURL("/post"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream(5000);
        for (int i = 0; i < 5000; i++) {
            baos.write(i);
        }
        Response response = target.request().post(Entity.entity(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));
        logger.info("The status of the response is " + response.getStatus());
        Assert.assertEquals(TestUtil.getErrorMessageForKnownIssue("JBEAP-2847"), Status.OK, response.getStatus());
        int counter = response.readEntity(int.class);

        response = target.request().post(Entity.entity(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));
        response.close();

        response = target.request().post(Entity.entity(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));
        response.close();

        response = target.request().post(Entity.entity(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));
        response.close();

        int updated = countTempFiles();
        logger.info("counter from server: " + counter);
        logger.info("counter updated: " + countTempFiles());
        Assert.assertTrue("The number of temporary files for datasource before and after request is not the same",
                counter > updated);
    }

    static int countTempFiles() throws Exception {
        String tmpdir = System.getProperty("java.io.tmpdir");
        Path dir = Paths.get(tmpdir);
        final AtomicInteger counter = new AtomicInteger(0);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "resteasy-provider-datasource*")) {
            stream.forEach(path -> counter.incrementAndGet());
        }
        return counter.intValue();
    }

    @AfterClass
    public static void afterclass() throws Exception {
        String tmpdir = System.getProperty("java.io.tmpdir");
        Path dir = Paths.get(tmpdir);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "resteasy-provider-datasource*")) {
            stream.forEach(path -> logger.info(path.toString()));
        }
    }
}
