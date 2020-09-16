package io.quarkus.rest.test.core.basic;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.UrlAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.core.basic.resource.ApplicationFilterCustomer;
import io.quarkus.rest.test.core.basic.resource.ApplicationFilterCustomerResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Configuration
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-541
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Application Filter Test")
public class ApplicationFilterTest {

    private static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            // war.addAsWebInfResource(ApplicationFilterTest.class.getPackage(), "ApplicationFilterWeb.xml", "web.xml");
            war.addClasses(ApplicationFilterCustomer.class, ApplicationFilterCustomerResource.class);
            war.add(new UrlAsset(ApplicationFilterTest.class.getResource("ApplicationFilter.html")), "foo.html");
            List<Class<?>> singletons = new ArrayList<>();
            singletons.add(ApplicationFilterCustomerResource.class);
            return TestUtil.finishContainerPrepare(war, null, singletons, (Class<?>[]) null);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ApplicationFilterTest.class.getSimpleName());
    }

    @BeforeEach
    public void setup() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Test for not founded method
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Method Not Found")
    public void testMethodNotFound() throws Exception {
        String newCustomer = "<customer>" + "<first-name>Bill</first-name>" + "<last-name>Burke</last-name>"
                + "<street>256 Clarendon Street</street>" + "<city>Boston</city>" + "<state>MA</state>" + "<zip>02115</zip>"
                + "<country>USA</country>" + "</customer>";
        Response response = client.target(generateURL("/customers")).request()
                .put(Entity.entity(newCustomer, "application/xml"));
        Assertions.assertEquals(405, response.getStatus());
    }

    /**
     * @tpTestDetails Test for getting html file that is not processed by resteasy
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Static Resource")
    public void testStaticResource() throws Exception {
        String response = client.target(generateURL("/foo.html")).request().get(String.class);
        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.indexOf("hello world") > -1);
    }

    /**
     * @tpTestDetails Test common application usage
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Customer Resource")
    public void testCustomerResource() throws Exception {
        // Create a new customer
        String newCustomer = "<customer>" + "<first-name>Bill</first-name>" + "<last-name>Burke</last-name>"
                + "<street>256 Clarendon Street</street>" + "<city>Boston</city>" + "<state>MA</state>" + "<zip>02115</zip>"
                + "<country>USA</country>" + "</customer>";
        URL postUrl = new URL(generateURL("/customers"));
        HttpURLConnection connection = (HttpURLConnection) postUrl.openConnection();
        connection.setDoOutput(true);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/xml");
        OutputStream os = connection.getOutputStream();
        os.write(newCustomer.getBytes());
        os.flush();
        Assertions.assertEquals(Status.CREATED.getStatusCode(), connection.getResponseCode());
        connection.disconnect();
        // Get the new customer
        URL getUrl = new URL(generateURL("/customers/1"));
        connection = (HttpURLConnection) getUrl.openConnection();
        connection.setRequestMethod("GET");
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line = reader.readLine();
        while (line != null) {
            line = reader.readLine();
        }
        Assertions.assertEquals(Status.OK.getStatusCode(), connection.getResponseCode());
        connection.disconnect();
        // Update the new customer.  Change Bill's name to William
        String updateCustomer = "<customer>" + "<first-name>William</first-name>" + "<last-name>Burke</last-name>"
                + "<street>256 Clarendon Street</street>" + "<city>Boston</city>" + "<state>MA</state>" + "<zip>02115</zip>"
                + "<country>USA</country>" + "</customer>";
        connection = (HttpURLConnection) getUrl.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", "application/xml");
        os = connection.getOutputStream();
        os.write(updateCustomer.getBytes());
        os.flush();
        Assertions.assertEquals(Status.NO_CONTENT.getStatusCode(), connection.getResponseCode());
        connection.disconnect();
        // Show the update
        connection = (HttpURLConnection) getUrl.openConnection();
        connection.setRequestMethod("GET");
        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        line = reader.readLine();
        while (line != null) {
            line = reader.readLine();
        }
        Assertions.assertEquals(Status.OK.getStatusCode(), connection.getResponseCode());
        connection.disconnect();
    }
}
