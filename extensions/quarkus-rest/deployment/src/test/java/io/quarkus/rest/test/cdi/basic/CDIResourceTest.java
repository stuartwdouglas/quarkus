package io.quarkus.rest.test.cdi.basic;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.logging.Logger;
import io.quarkus.rest.test.util.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.quarkus.rest.test.ContainerConstants;
import io.quarkus.rest.test.cdi.basic.resource.resteasy1082.FooResource;
import io.quarkus.rest.test.cdi.basic.resource.resteasy1082.TestApplication;
import io.quarkus.rest.test.cdi.basic.resource.resteasy1082.TestServlet;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1082
 * @tpSince RESTEasy 3.0.16
 *
 *          Jul 27, 2018 Test rewritten to generated the needed archive and write it to disk.
 */

public class CDIResourceTest {

    protected static final Logger logger = Logger.getLogger(CDIResourceTest.class.getName());

    private static final String WAR_NAME = "RESTEASY-1082.war";
    static final String toStr;
    static final File exportFile;

    static {
        toStr = new StringBuilder()
                .append(TestUtil.getStandaloneDir(ContainerConstants.DEFAULT_CONTAINER_QUALIFIER)).append(File.separator)
                .append("deployments").append(File.separator)
                .append(WAR_NAME).toString();
        exportFile = new File(FileSystems.getDefault().getPath("target").toFile(), WAR_NAME);
    }

    @Before
    public void createArchive() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, WAR_NAME);
        war.addClasses(FooResource.class,
                TestApplication.class,
                TestServlet.class);

//        war.addAsWebInfResource(CDIResourceTest.class.getPackage(),
//                "web-resteasy1082.xml", "web.xml");

        //write file to disk
        war.as(ZipExporter.class).exportTo(exportFile, true);
    }

    /**
     * @tpTestDetails Redeploy deployment with RESTEasy and CDI beans. Check errors.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testCDIResourceFromServlet() throws Exception {
        Path from = FileSystems.getDefault().getPath(exportFile.getAbsolutePath());
        Path to = FileSystems.getDefault().getPath(toStr).toAbsolutePath();

        try {
            // Delete existing RESTEASY-1082.war, if any.
            try {
                Files.delete(to);
            } catch (Exception e) {
                // ok
            }

            // Deploy RESTEASY-1082.war
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Copied war to " + to);
            CloseableHttpClient client = HttpClients.createDefault();
            HttpGet get = new HttpGet(PortProviderUtil.generateURL("/test", "RESTEASY-1082"));

            // Wait for RESTEASY-1082.war to be installed.
            HttpResponse response = client.execute(get);
            boolean succesInDeploy = false;
            for (int i = 0; i < 40; i++) {
                get.releaseConnection();
                response = client.execute(get);
                if (response.getStatusLine().getStatusCode() != Status.NOT_FOUND.getStatusCode()) {
                    succesInDeploy = true;
                    break;
                }
                Thread.sleep(TimeoutUtil.adjust(500));
            }
            Assert.assertTrue("Deployment was not deployed", succesInDeploy);
            logger.info("status: " + response.getStatusLine().getStatusCode());
            printResponse(response);
            Assert.assertEquals(Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
            get.releaseConnection();

            // Redeploy RESTEASY-1082.war
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Replaced war");
            Thread.sleep(TimeoutUtil.adjust(5000));

            // Wait for RESTEASY-1082.war to be installed.
            response = client.execute(get);
            succesInDeploy = false;
            for (int i = 0; i < 40; i++) {
                get.releaseConnection();
                response = client.execute(get);
                if (response.getStatusLine().getStatusCode() != Status.NOT_FOUND.getStatusCode()) {
                    succesInDeploy = true;
                    break;
                }
                Thread.sleep(TimeoutUtil.adjust(500));
            }
            Assert.assertTrue("Deployment was not deployed", succesInDeploy);

            logger.info("status: " + response.getStatusLine().getStatusCode());
            printResponse(response);
            Assert.assertEquals(Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
        } finally {
            Files.delete(to);
        }
    }

    protected void printResponse(HttpResponse response) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String line = reader.readLine();
        logger.info("_____Response:_____");
        while (line != null) {
            logger.info(line);
            line = reader.readLine();
        }
        logger.info("___________________");
    }
}
