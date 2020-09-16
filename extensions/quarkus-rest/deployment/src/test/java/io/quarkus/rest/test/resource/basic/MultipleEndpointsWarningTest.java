package io.quarkus.rest.test.resource.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.resource.basic.resource.LogHandler;
import io.quarkus.rest.test.resource.basic.resource.MultipleEndpointsWarningResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resources
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression tests for RESTEASY-1398
 * @tpSince RESTEasy 3.0.20
 */
@DisplayName("Multiple Endpoints Warning Test")
public class MultipleEndpointsWarningTest {

    private static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(LogHandler.class);
            // Test registers it's own LogHandler
            return TestUtil.finishContainerPrepare(war, null, MultipleEndpointsWarningResource.class);
        }
    });

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, MultipleEndpointsWarningTest.class.getSimpleName());
    }

    @BeforeAll
    public static void setUp() throws Exception {
        client = ClientBuilder.newClient();
        client.target(generateURL("/setup")).request().get();
    }

    @AfterAll
    public static void tearDown() {
        client.target(generateURL("/teardown")).request().get();
        client.close();
    }

    @Test
    @DisplayName("Test Unique")
    public void testUnique() throws Exception {
        Response response = client.target(generateURL("/unique/")).request().accept(MediaType.TEXT_PLAIN).get();
        Assertions.assertEquals("Incorrectly logged " + LogHandler.MESSAGE_CODE, new Long(0), response.readEntity(long.class));
        response = client.target(generateURL("/unique")).request().get();
        Assertions.assertEquals("Incorrectly logged " + LogHandler.MESSAGE_CODE, new Long(0), response.readEntity(long.class));
        response = client.target(generateURL("/unique")).request().accept(MediaType.TEXT_PLAIN).get();
        Assertions.assertEquals("Incorrectly logged " + LogHandler.MESSAGE_CODE, new Long(0), response.readEntity(long.class));
        response = client.target(generateURL("/1")).request().get();
        Assertions.assertEquals("Incorrectly logged " + LogHandler.MESSAGE_CODE, new Long(0), response.readEntity(long.class));
    }

    @Test
    @DisplayName("Test Different Verbs")
    public void testDifferentVerbs() throws Exception {
        Response response = client.target(generateURL("/verbs")).request().accept(MediaType.TEXT_PLAIN).get();
        Assertions.assertEquals("Incorrectly logged " + LogHandler.MESSAGE_CODE, new Long(0), response.readEntity(long.class));
        response = client.target(generateURL("/verbs")).request().accept(MediaType.TEXT_PLAIN, MediaType.WILDCARD).get();
        Assertions.assertEquals("Incorrectly logged " + LogHandler.MESSAGE_CODE, new Long(0), response.readEntity(long.class));
        response = client.target(generateURL("/verbs")).request().get();
        Assertions.assertEquals("Incorrectly logged " + LogHandler.MESSAGE_CODE, new Long(0), response.readEntity(long.class));
    }

    @Test
    @DisplayName("Test Duplicate")
    public void testDuplicate() throws Exception {
        Response response = client.target(generateURL("/duplicate")).request().get();
        Assertions.assertEquals(LogHandler.MESSAGE_CODE + " should've been logged once", new Long(1),
                response.readEntity(long.class));
    }
}
