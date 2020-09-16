package io.quarkus.rest.test.response;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.internal.QuarkusRestClientBuilderImpl;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.response.resource.ResponseTrimmingResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Response
 * @tpChapter Integration tests
 * @tpTestCaseDetails Ensures that response is not too long after endpoint consumes big invalid data (see
 *                    https://issues.jboss.org/browse/JBEAP-6316)
 * @tpSince RESTEasy 3.6.1
 */
@DisplayName("Response Trimming Test")
public class ResponseTrimmingTest {

    static Client client;

    private static String original;

    private static String trimmed;

    private static final String DEFAULT = "war_default";

    private static final String NO_JSON_B = "war_no_json_b";

    /**
     * Prepare deployment with default configuration. JSON-B will be used.
     */
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, ResponseTrimmingResource.class);
        }
    });

    /**
     * Prepare deployment with jboss-deployment-structure-no-json-b.xml. Jackson will be used.
     */
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addAsManifestResource("jboss-deployment-structure-no-json-b.xml", "jboss-deployment-structure.xml");
            return TestUtil.finishContainerPrepare(war, null, ResponseTrimmingResource.class);
        }
    });

    /**
     * Prepare string for tests and its trimmed version.
     */
    @BeforeAll
    public static void init() {
        client = new QuarkusRestClientBuilderImpl().build();
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 1024; i++) {
            sb.append("A");
        }
        original = sb.toString();
        StringBuilder sb2 = new StringBuilder();
        for (int i = 1; i <= 256; i++) {
            sb2.append("A");
        }
        trimmed = sb2.toString();
    }

    @AfterAll
    public static void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Test long error message trimming with JsonB
     * @tpSince RESTEasy 3.6.1.Final
     */
    @Test
    @DisplayName("Test Default")
    public void testDefault() {
        test(DEFAULT);
    }

    /**
     * @tpTestDetails Test long error message trimming with Jackson
     * @tpSince RESTEasy 3.6.1.Final
     */
    @Test
    @DisplayName("Test No Json B")
    public void testNoJsonB() {
        test(NO_JSON_B);
    }

    /**
     * Send long string to the endpoint that expects int so error message is returned in response.
     * Check that response does not contain full string and has reasonable length.
     *
     * @param deployment DEFAULT (use JSON-B) or NO_JSON_B (use Jackson)
     */
    private void test(String deployment) {
        Response response = client.target(PortProviderUtil.generateURL("/json", deployment)).request()
                .post(Entity.entity(original, "application/json"));
        String responseText = response.readEntity(String.class);
        if (deployment.equals(NO_JSON_B)) {
            Assertions.assertTrue(responseText.contains(trimmed), "Unrecognized token does not show");
            Assertions.assertFalse(responseText.contains(trimmed.concat("A")), "Unrecognized token is not reasonably trimmed");
        }
        Assertions.assertTrue(responseText.length() <= 550, "Response is longer than 550 characters");
        response.close();
    }
}
