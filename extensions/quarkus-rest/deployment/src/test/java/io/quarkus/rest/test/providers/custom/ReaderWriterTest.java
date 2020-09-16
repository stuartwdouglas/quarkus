package io.quarkus.rest.test.providers.custom;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.resteasy.client.jaxrs.ProxyBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.custom.resource.ReaderWriterClient;
import io.quarkus.rest.test.providers.custom.resource.ReaderWriterCurlyBraces;
import io.quarkus.rest.test.providers.custom.resource.ReaderWriterCustomer;
import io.quarkus.rest.test.providers.custom.resource.ReaderWriterCustomerWriter;
import io.quarkus.rest.test.providers.custom.resource.ReaderWriterHignPriorityCustomerWriter;
import io.quarkus.rest.test.providers.custom.resource.ReaderWriterLowPriorityCustomerWriter;
import io.quarkus.rest.test.providers.custom.resource.ReaderWriterNowhereClient;
import io.quarkus.rest.test.providers.custom.resource.ReaderWriterResource;
import io.quarkus.rest.test.providers.custom.resource.ReaderWriterSpaces;
import io.quarkus.rest.test.providers.custom.resource.ReaderWriterSub;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Providers
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEasy issues
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Reader Writer Test")
public class ReaderWriterTest {

    static QuarkusRestClient client;

    static final String PriorityDeploymenetName = "ReaderWriterCustomerWriterWithPriority";

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(ReaderWriterCustomer.class);
            war.addClass(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, ReaderWriterCustomerWriter.class, ReaderWriterResource.class);
        }
    });

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, ReaderWriterResource.class);
        }
    });

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(ReaderWriterSub.class);
            war.addClass(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, ReaderWriterSpaces.class);
        }
    });

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, ReaderWriterCurlyBraces.class);
        }
    });

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(ReaderWriterCustomer.class);
            war.addClass(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, ReaderWriterLowPriorityCustomerWriter.class,
                    ReaderWriterCustomerWriter.class, ReaderWriterHignPriorityCustomerWriter.class, ReaderWriterResource.class);
        }
    });

    @BeforeAll
    public static void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterAll
    public static void close() {
        client.close();
    }

    /**
     * @tpTestDetails Regression test for RESTEASY-144
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test 144")
    public void test144() throws Exception {
        WebTarget base = client
                .target(PortProviderUtil.generateURL("/implicit", ReaderWriterCustomerWriter.class.getSimpleName()));
        Response response = base.request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.getStringHeaders().getFirst("content-type"), "application/xml;charset=UTF-8");
        String s = new String(response.readEntity(byte[].class), "US-ASCII");
        Assertions.assertEquals("<customer><name>bill</name></customer>", s, "Response contains wrong content");
        response.close();
        response = base.request().delete();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
        ReaderWriterClient proxy = client
                .target(PortProviderUtil.generateBaseUrl(ReaderWriterCustomerWriter.class.getSimpleName()))
                .proxy(ReaderWriterClient.class);
        response = proxy.deleteCustomer();
        response.close();
        response = proxy.deleteComplex();
        Assertions.assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Regression test for RESTEASY-61
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Jdk URL Connection")
    public void testJdkURLConnection() throws Exception {
        URL url = new URL(PortProviderUtil.generateURL("/simple", ReaderWriterResource.class.getSimpleName()));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        @SuppressWarnings("unused")
        Object obj = conn.getContent();
    }

    /**
     * @tpTestDetails Regression test for RESTEASY-24 and RESTEASY-139
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test 24")
    public void test24() throws Exception {
        WebTarget base = client.target(PortProviderUtil.generateURL("/complex", ReaderWriterResource.class.getSimpleName()));
        Response response = base.request().get();
        Assertions.assertEquals(Status.FOUND.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.getStringHeaders().getFirst("content-type"), "text/plain;charset=UTF-8");
        byte[] responseBody = response.readEntity(byte[].class);
        String responseString = new String(responseBody, "US-ASCII");
        Assertions.assertEquals("hello world", responseString, "Response contains wrong content");
    }

    /**
     * @tpTestDetails Regression test for RESTEASY-1 and RESTEASY-2
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test 1 and 2")
    public void test1and2() throws Exception {
        WebTarget base = client.target(PortProviderUtil.generateURL("/simple", ReaderWriterResource.class.getSimpleName()));
        Response response = base.request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.getStringHeaders().getFirst("content-type"), "text/plain;charset=UTF-8");
        String s = new String(response.readEntity(byte[].class), "US-ASCII");
        Assertions.assertEquals("hello world", s, "Response contains wrong content");
    }

    /**
     * @tpTestDetails Test for resource mapped to nowhere using proxy.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test It New Proxy")
    public void testItNewProxy() throws Exception {
        ReaderWriterNowhereClient proxy = ProxyBuilder
                .builder(ReaderWriterNowhereClient.class, client.target(PortProviderUtil.generateBaseUrl("app"))).build();
        Response response = proxy.read();
        response.close();
    }

    /**
     * @tpTestDetails Regression test for RESTEASY-212
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test 212")
    public void test212() throws Exception {
        WebTarget base = client.target(
                PortProviderUtil.generateURL("/spaces/with%20spaces/without", ReaderWriterSpaces.class.getSimpleName()));
        Response response = base.request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Regression test for RESTEASY-227
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @OperateOnDeployment("ReaderWriterCurlyBraces")
    @DisplayName("Test 227")
    public void test227() throws Exception {
        WebTarget base = client
                .target(PortProviderUtil.generateURL("/curly/abcd", ReaderWriterCurlyBraces.class.getSimpleName()));
        Response response = base.request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
    }

    @Test
    @OperateOnDeployment(PriorityDeploymenetName)
    @DisplayName("Test Provider With Priority")
    public void testProviderWithPriority() throws Exception {
        WebTarget base = client.target(PortProviderUtil.generateURL("/priority", PriorityDeploymenetName));
        Response response = base.request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.getStringHeaders().getFirst("content-type"), "application/xml;charset=UTF-8");
        String s = new String(response.readEntity(byte[].class), "UTF-8");
        Assertions.assertEquals("<customer><name>high priority</name></customer>", s, "Response contains wrong content");
        response.close();
    }
}
