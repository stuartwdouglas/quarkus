package io.quarkus.rest.test.form;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.form.resource.FormUrlEncodedCharsetResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Form tests
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for JBEAP-4693
 * @tpSince RESTEasy 3.0.17
 */
@DisplayName("Form Url Encoded Charset Test")
public class FormUrlEncodedCharsetTest {

    protected static MediaType testMediaType8 = MediaType.APPLICATION_FORM_URLENCODED_TYPE
            .withCharset(StandardCharsets.UTF_8.displayName());

    protected static MediaType testMediaType16 = MediaType.APPLICATION_FORM_URLENCODED_TYPE
            .withCharset(StandardCharsets.UTF_16.displayName());

    protected static String alephBetGimel = "אבג";

    private static Client client;

    private static WebTarget target;

    protected static final Logger logger = Logger.getLogger(FormUrlEncodedCharsetTest.class.getName());

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, FormUrlEncodedCharsetResource.class);
        }
    });

    @BeforeAll
    public static void init() {
        client = ClientBuilder.newClient();
        target = client.target(PortProviderUtil.generateURL("/test", FormUrlEncodedCharsetTest.class.getSimpleName()));
    }

    @AfterAll
    public static void end() {
        client.close();
    }

    /**
     * @tpTestDetails Test for default charset.
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    @DisplayName("Test Form Default")
    public void testFormDefault() throws UnsupportedEncodingException {
        Form form = new Form();
        form.param("name", alephBetGimel);
        Entity<Form> entity = Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response response = target.request().post(entity);
        String result = response.readEntity(String.class);
        logger.info("result: " + result);
        Assertions.assertEquals(result, alephBetGimel, "EAP is unable to encode default charset");
    }

    /**
     * @tpTestDetails Test for UTF8 charset.
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    @DisplayName("Test Form UTF 8")
    public void testFormUTF8() throws UnsupportedEncodingException {
        Form form = new Form();
        form.param("name", alephBetGimel);
        Entity<Form> entity = Entity.entity(form, testMediaType8);
        Response response = target.request().post(entity);
        String result = response.readEntity(String.class);
        logger.info("result: " + result);
        Assertions.assertEquals(result, alephBetGimel, "EAP is unable to encode UTF8 charset");
    }

    /**
     * @tpTestDetails Test for UTF16 charset.
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    @DisplayName("Test Form UTF 16")
    public void testFormUTF16() throws UnsupportedEncodingException {
        Form form = new Form();
        form.param("name", alephBetGimel);
        Entity<Form> entity = Entity.entity(form, testMediaType16);
        Response response = target.request().post(entity);
        String result = response.readEntity(String.class);
        logger.info("result: " + result);
        Assertions.assertEquals(result, alephBetGimel, "EAP is unable to encode UTF16 charset");
    }
}
