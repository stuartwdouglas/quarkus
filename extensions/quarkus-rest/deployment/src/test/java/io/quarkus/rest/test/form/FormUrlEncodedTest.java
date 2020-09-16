package io.quarkus.rest.test.form;

import java.util.function.Supplier;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
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

import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.runtime.util.MultivaluedMapImpl;
import io.quarkus.rest.test.form.resource.FormUrlEncodedResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Form tests
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Form Url Encoded Test")
public class FormUrlEncodedTest {

    private static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, FormUrlEncodedResource.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, FormUrlEncodedTest.class.getSimpleName());
    }

    @BeforeAll
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Get form parameter from resource using InputStream and StreamingOutput
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Post")
    public void testPost() {
        WebTarget base = client.target(generateURL("/simple"));
        Response response = base.request().post(Entity.form(new Form().param("hello", "world")));
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String body = response.readEntity(String.class);
        Assertions.assertEquals(body, "Wrong response content", "hello=world");
        response.close();
    }

    /**
     * @tpTestDetails Send form with an empty parameter value.
     * @tpSince RESTEasy 3.0.20
     */
    @Test
    @DisplayName("Test Resteasy 109")
    public void testResteasy109() {
        Builder builder = client.target(generateURL("/RESTEASY-109")).request();
        Response response = null;
        try {
            response = builder.post(
                    Entity.entity("name=jon&address1=123+Main+St&address2=&zip=12345", MediaType.APPLICATION_FORM_URLENCODED));
            Assertions.assertEquals(204, response.getStatus());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            response.close();
        }
    }

    /**
     * @tpTestDetails Send form with a missing query parameter.
     * @tpSince RESTEasy 3.0.20
     */
    @Test
    @DisplayName("Test Query Param Is Null")
    public void testQueryParamIsNull() {
        Builder builder = client.target(generateURL("/simple")).request();
        try {
            Response response = builder.post(Entity.form(new Form("hello", "world")));
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals(response.readEntity(String.class), "hello=world");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @tpTestDetails Send form with two parameters.
     * @tpSince RESTEasy 3.0.20
     */
    @Test
    @DisplayName("Test Post Two Parameters")
    public void testPostTwoParameters() {
        Builder builder = client.target(generateURL("/form/twoparams")).request();
        try {
            Response response = builder.post(Entity.form(new Form("hello", "world").param("yo", "mama")));
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            String body = response.readEntity(String.class);
            Assertions.assertTrue(body.indexOf("hello=world") != -1);
            Assertions.assertTrue(body.indexOf("yo=mama") != -1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Path("/")
    @DisplayName("Test Proxy")
    public interface TestProxy {

        @Path("/form")
        @POST
        @Produces("application/x-www-form-urlencoded")
        @Consumes("application/x-www-form-urlencoded")
        String post(MultivaluedMap<String, String> form);

        @Path("/form")
        @POST
        @Produces("application/x-www-form-urlencoded")
        @Consumes("application/x-www-form-urlencoded")
        MultivaluedMap<String, String> post2(MultivaluedMap<String, String> form);
    }

    /**
     * @tpTestDetails Send form by proxy.
     * @tpSince RESTEasy 3.0.20
     */
    @Test
    @DisplayName("Test Proxy")
    public void testProxy() {
        QuarkusRestWebTarget target = (QuarkusRestWebTarget) client.target(generateURL(""));
        TestProxy proxy = target.proxy(TestProxy.class);
        MultivaluedMapImpl<String, String> form = new MultivaluedMapImpl<String, String>();
        form.add("hello", "world");
        String body = proxy.post(form);
        Assertions.assertEquals(body, "hello=world");
        MultivaluedMap<String, String> rtn = proxy.post2(form);
        Assertions.assertEquals(rtn.getFirst("hello"), "world");
    }
}
