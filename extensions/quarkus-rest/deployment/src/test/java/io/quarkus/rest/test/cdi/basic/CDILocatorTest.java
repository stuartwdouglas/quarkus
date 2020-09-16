package io.quarkus.rest.test.cdi.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.cdi.basic.resource.CDILocatorResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for CDI locator
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Cdi Locator Test")
public class CDILocatorTest {

    static Client client;

    static WebTarget baseTarget;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, CDILocatorResource.class);
        }
    });

    private static String generateURL() {
        return PortProviderUtil.generateBaseUrl(CDILocatorTest.class.getSimpleName());
    }

    @BeforeAll
    public static void initClient() {
        client = ClientBuilder.newClient();
        baseTarget = client.target(generateURL());
    }

    @AfterAll
    public static void closeClient() {
        client.close();
    }

    /**
     * @tpTestDetails Check generic type
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Generic Type Test")
    public void genericTypeTest() throws Exception {
        String result = baseTarget.path("test").queryParam("foo", "yo").request().get(String.class);
        Assertions.assertEquals("OK", result, "Wrong response");
    }

    /**
     * @tpTestDetails Check locator
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Locator Test")
    public void locatorTest() throws Exception {
        String result = baseTarget.path("test/lookup").queryParam("foo", "yo").request().get(String.class);
        Assertions.assertEquals("OK", result, "Wrong response");
    }
}
