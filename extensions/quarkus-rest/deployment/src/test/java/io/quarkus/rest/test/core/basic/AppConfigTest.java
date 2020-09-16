package io.quarkus.rest.test.core.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.core.basic.resource.AppConfigApplication;
import io.quarkus.rest.test.core.basic.resource.AppConfigResources;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Configuration
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for resource and provider defined in one class together.
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("App Config Test")
public class AppConfigTest {

    private static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(AppConfigResources.class);
            war.addClass(AppConfigApplication.class);
            // war.addAsWebInfResource(AppConfigTest.class.getPackage(), "AppConfigWeb.xml", "web.xml");
            return war;
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, AppConfigTest.class.getSimpleName());
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
     * @tpTestDetails Test for apache client
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Apache Client")
    public void apacheClient() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(generateURL("/my"));
        CloseableHttpResponse response1 = httpclient.execute(httpGet);
        try {
            Assertions.assertEquals(Status.OK.getStatusCode(), response1.getStatusLine().getStatusCode());
            Assertions.assertEquals(TestUtil.readString(response1.getEntity().getContent()), "\"hello\"");
        } finally {
            response1.close();
        }
    }
}
