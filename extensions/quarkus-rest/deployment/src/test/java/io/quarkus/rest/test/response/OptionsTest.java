package io.quarkus.rest.test.response;

import java.util.HashSet;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.response.resource.OptionParamsResource;
import io.quarkus.rest.test.response.resource.OptionUsersResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Response
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-363
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Options Test")
public class OptionsTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, OptionParamsResource.class, OptionUsersResource.class);
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

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, OptionsTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Check options HTTP request
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Options")
    public void testOptions() throws Exception {
        WebTarget base = client.target(generateURL("/params/customers/333/phonenumbers"));
        Response response = base.request().options();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Check not allowed request
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Method Not Allowed")
    public void testMethodNotAllowed() throws Exception {
        WebTarget base = client.target(generateURL("/params/customers/333/phonenumbers"));
        Response response = base.request().post(Entity.text(new String()));
        Assertions.assertEquals(Status.METHOD_NOT_ALLOWED.getStatusCode(), response.getStatus());
        response.close();
        base = client.target(generateURL("/users"));
        response = base.request().delete();
        Assertions.assertEquals(Status.METHOD_NOT_ALLOWED.getStatusCode(), response.getStatus());
        response.close();
        base = client.target(generateURL("/users/53"));
        response = base.request().post(Entity.text(new String()));
        Assertions.assertEquals(Status.METHOD_NOT_ALLOWED.getStatusCode(), response.getStatus());
        response.close();
        base = client.target(generateURL("/users/53/contacts"));
        response = base.request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
        base = client.target(generateURL("/users/53/contacts"));
        response = base.request().options();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
        base = client.target(generateURL("/users/53/contacts"));
        response = base.request().delete();
        Assertions.assertEquals(Status.METHOD_NOT_ALLOWED.getStatusCode(), response.getStatus());
        response.close();
        base = client.target(generateURL("/users/53/contacts/carl"));
        response = base.request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
        base = client.target(generateURL("/users/53/contacts/carl"));
        response = base.request().post(Entity.text(new String()));
        Assertions.assertEquals(Status.METHOD_NOT_ALLOWED.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Check Allow header on 200 status
     * @tpSince RESTEasy 3.0.20
     */
    @Test
    @DisplayName("Test Allow Header OK")
    public void testAllowHeaderOK() {
        WebTarget base = client.target(generateURL("/users/53/contacts"));
        Response response = base.request().options();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        checkOptions(response, "GET", "POST", "HEAD", "OPTIONS");
        response.close();
    }

    /**
     * @tpTestDetails Check Allow header on 405 status
     * @tpSince RESTEasy 3.0.20
     */
    @Test
    @DisplayName("Test Allow Header Method Not Allowed")
    public void testAllowHeaderMethodNotAllowed() {
        WebTarget base = client.target(generateURL("/params/customers/333/phonenumbers"));
        Response response = base.request().post(Entity.text(new String()));
        Assertions.assertEquals(Status.METHOD_NOT_ALLOWED.getStatusCode(), response.getStatus());
        checkOptions(response, "GET", "HEAD", "OPTIONS");
        response.close();
    }

    private void checkOptions(Response response, String... verbs) {
        String allowed = response.getHeaderString("Allow");
        Assertions.assertNotNull(allowed);
        HashSet<String> vals = new HashSet<String>();
        for (String v : allowed.split(",")) {
            vals.add(v.trim());
        }
        Assertions.assertEquals(verbs.length, vals.size());
        for (String verb : verbs) {
            Assertions.assertTrue(vals.contains(verb));
        }
    }
}
