package io.quarkus.rest.test.form;

import static io.quarkus.rest.test.Assert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.form.resource.FormParameterResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Form tests
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-760
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Form Parameter Test")
public class FormParameterTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(FormParameterTest.class);
            return TestUtil.finishContainerPrepare(war, null, FormParameterResource.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, FormParameterTest.class.getSimpleName());
    }

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
        client = null;
    }

    /**
     * @tpTestDetails Client sends PUT requests.
     *                Form parameter is used and should be returned by RE resource.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Form Param With No Query Param Put")
    public void testFormParamWithNoQueryParamPut() throws Exception {
        Form form = new Form();
        form.param("formParam", "abc xyz");
        Response response = client.target(generateURL("/put/noquery/")).request()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        assertThat("Wrong response", response, notNullValue());
        response.bufferEntity();
        assertEquals(response.readEntity(String.class), "Wrong response", "abc xyz");
    }

    /**
     * @tpTestDetails Client sends PUT requests.
     *                Form parameter is used (encoded) and should be returned by RE resource.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Form Param With No Query Param Put Encoded")
    public void testFormParamWithNoQueryParamPutEncoded() throws Exception {
        Form form = new Form();
        form.param("formParam", "abc xyz");
        Response response = client.target(generateURL("/put/noquery/encoded")).request()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        assertThat("Wrong response", response, notNullValue());
        response.bufferEntity();
        assertEquals(response.readEntity(String.class), "Wrong response", "abc+xyz");
    }

    /**
     * @tpTestDetails Client sends POST requests.
     *                Form parameter is used and should be returned by RE resource.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Form Param With No Query Param Post")
    public void testFormParamWithNoQueryParamPost() throws Exception {
        Form form = new Form();
        form.param("formParam", "abc xyz");
        Response response = client.target(generateURL("/post/noquery/")).request()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        assertThat("Wrong response", response, notNullValue());
        response.bufferEntity();
        assertEquals(response.readEntity(String.class), "Wrong response", "abc xyz");
    }

    /**
     * @tpTestDetails Client sends POST requests.
     *                Form parameter is used (encoded) and should be returned by RE resource.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Form Param With No Query Param Post Encoded")
    public void testFormParamWithNoQueryParamPostEncoded() throws Exception {
        Form form = new Form();
        form.param("formParam", "abc xyz");
        Response response = client.target(generateURL("/post/noquery/encoded")).request()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        assertThat("Wrong response", response, notNullValue());
        response.bufferEntity();
        assertEquals(response.readEntity(String.class), "Wrong response", "abc+xyz");
    }

    /**
     * @tpTestDetails Client sends PUT requests. Query parameter is used.
     *                Form parameter is used too and should be returned by RE resource.
     *                This is regression test for JBEAP-982
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Form Param With Query Param Put")
    public void testFormParamWithQueryParamPut() throws Exception {
        Form form = new Form();
        form.param("formParam", "abc xyz");
        Response response = client.target(generateURL("/put/query?query=xyz")).request()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        assertThat("Wrong response", response, notNullValue());
        response.bufferEntity();
        assertEquals(response.readEntity(String.class), "Wrong response", "abc xyz");
    }

    /**
     * @tpTestDetails Client sends PUT requests. Query parameter is used.
     *                Form parameter is used too (encoded) and should be returned by RE resource.
     *                This is regression test for JBEAP-982
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Form Param With Query Param Put Encoded")
    public void testFormParamWithQueryParamPutEncoded() throws Exception {
        Form form = new Form();
        form.param("formParam", "abc xyz");
        Response response = client.target(generateURL("/put/query/encoded?query=xyz")).request()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        assertThat("Wrong response", response, notNullValue());
        response.bufferEntity();
        assertEquals(response.readEntity(String.class), "Wrong response", "abc+xyz");
    }

    /**
     * @tpTestDetails Client sends POST requests. Query parameter is used.
     *                Form parameter is used too and should be returned by RE resource.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Form Param With Query Param Post")
    public void testFormParamWithQueryParamPost() throws Exception {
        Form form = new Form();
        form.param("formParam", "abc xyz");
        Response response = client.target(generateURL("/post/query?query=xyz")).request()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        assertThat("Wrong response", response, notNullValue());
        response.bufferEntity();
        assertEquals(response.readEntity(String.class), "Wrong response", "abc xyz");
    }

    /**
     * @tpTestDetails Client sends POST requests. Query parameter is used.
     *                Form parameter is used too (encoded) and should be returned by RE resource.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Form Param With Query Param Post Encoded")
    public void testFormParamWithQueryParamPostEncoded() throws Exception {
        Form form = new Form();
        form.param("formParam", "abc xyz");
        Response response = client.target(generateURL("/post/query/encoded?query=xyz")).request()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        assertThat("Wrong response", response, notNullValue());
        response.bufferEntity();
        assertEquals(response.readEntity(String.class), "Wrong response", "abc+xyz");
    }
}
