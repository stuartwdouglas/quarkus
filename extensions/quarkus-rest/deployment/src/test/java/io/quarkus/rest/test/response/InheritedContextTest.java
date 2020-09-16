package io.quarkus.rest.test.response;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
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

import io.quarkus.rest.test.response.resource.InheritedContextNewService;
import io.quarkus.rest.test.response.resource.InheritedContextNewSubService;
import io.quarkus.rest.test.response.resource.InheritedContextService;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Response
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-952
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Inherited Context Test")
public class InheritedContextTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, InheritedContextService.class, InheritedContextNewService.class,
                    InheritedContextNewSubService.class);
        }
    });

    protected Client client;

    @BeforeEach
    public void beforeTest() {
        client = ClientBuilder.newClient();
    }

    @AfterEach
    public void afterTest() {
        client.close();
        client = null;
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, InheritedContextTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test basic resource with no inheritance
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Context")
    public void testContext() throws Exception {
        Invocation.Builder request = client.target(generateURL("/super/test/BaseService")).request();
        Response response = request.get();
        String s = response.readEntity(String.class);
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(s, "true");
        response.close();
    }

    /**
     * @tpTestDetails Test basic resource with one level of inheritance
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Inherited Context One Level")
    public void testInheritedContextOneLevel() throws Exception {
        Invocation.Builder request = client.target(generateURL("/sub/test/SomeService")).request();
        Response response = request.get();
        String s = response.readEntity(String.class);
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(s, "true");
        response.close();
    }

    /**
     * @tpTestDetails Test basic resource with two levels of inheritance
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Inherited Context Two Levels")
    public void testInheritedContextTwoLevels() throws Exception {
        Invocation.Builder request = client.target(generateURL("/subsub/test/SomeSubService")).request();
        Response response = request.get();
        String s = response.readEntity(String.class);
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(s, "true");
        response.close();
    }
}
