package io.quarkus.rest.test.form;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.form.resource.FormEntityResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Form tests
 * @tpChapter Integration tests
 * @tpSince RESTEasy 4.0.0
 */
@DisplayName("Form Entity Test")
public class FormEntityTest {

    private static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, FormEntityResource.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, FormEntityTest.class.getSimpleName());
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
     * @tpTestDetails Retrieve form param and form entity
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    @DisplayName("Test With Equals And Empty String")
    public void testWithEqualsAndEmptyString() throws Exception {
        Invocation.Builder request = client.target(generateURL("/test/form")).request();
        Response response = request.post(Entity.entity("fp=abc&fp2=\"\"", "application/x-www-form-urlencoded"));
        String s = response.readEntity(String.class);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertTrue(s.equals("abc|fp=abc&fp2=\"\"") || s.equals("abc|fp2=\"\"&fp=abc"));
    }

    /**
     * @tpTestDetails Retrieve form param and form entity
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    @DisplayName("Test With Equals")
    public void testWithEquals() throws Exception {
        Invocation.Builder request = client.target(generateURL("/test/form")).request();
        Response response = request.post(Entity.entity("fp=abc&fp2=", "application/x-www-form-urlencoded"));
        String s = response.readEntity(String.class);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertTrue(s.equals("abc|fp=abc&fp2") || s.equals("abc|fp2&fp=abc"));
    }

    /**
     * @tpTestDetails Retrieve form param and form entity
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    @DisplayName("Test Without Equals")
    public void testWithoutEquals() throws Exception {
        Invocation.Builder request = client.target(generateURL("/test/form")).request();
        Response response = request.post(Entity.entity("fp=abc&fp2", "application/x-www-form-urlencoded"));
        String s = response.readEntity(String.class);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertTrue(s.equals("abc|fp=abc&fp2") || s.equals("abc|fp2&fp=abc"));
    }
}
