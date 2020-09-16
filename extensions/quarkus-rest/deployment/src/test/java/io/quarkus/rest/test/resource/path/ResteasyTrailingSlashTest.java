package io.quarkus.rest.test.resource.path;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.resource.path.resource.ResteasyTrailingSlashResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 * @tpTestCaseDetails Check for slash in URL
 */
@DisplayName("Resteasy Trailing Slash Test")
public class ResteasyTrailingSlashTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, ResteasyTrailingSlashResource.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ResteasyTrailingSlashTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Client should accept also URL ended by slash
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Trailing Slash")
    public void testTrailingSlash() throws Exception {
        Client client = ClientBuilder.newClient();
        String val = client.target(generateURL("/test/")).request().get(String.class);
        assertEquals("hello world", val, "Wrong response");
        client.close();
    }
}
