package io.quarkus.rest.test.core.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.core.basic.resource.FileExtensionMappingApplication;
import io.quarkus.rest.test.core.basic.resource.FileExtensionMappingResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter MediaType
 * @tpChapter Integration tests
 * @tpTestCaseDetails Mapping file extensions to media types
 * @tpSince RESTEasy 3.0.20
 */
@DisplayName("File Extension Mapping Test")
public class FileExtensionMappingTest {

    static Client client;

    @BeforeAll
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void close() {
        client.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(FileExtensionMappingApplication.class);
            // war.addAsWebInfResource(FileExtensionMappingTest.class.getPackage(), "FileExtensionMapping.xml", "web.xml");
            JavaArchive archive = TestUtil.finishContainerPrepare(war, null, FileExtensionMappingResource.class);
            return archive;
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, FileExtensionMappingTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Map suffix .txt to Accept: text/plain
     * @tpSince RESTEasy 3.0.20
     */
    @Test
    @DisplayName("Test File Extension Mapping Plain")
    public void testFileExtensionMappingPlain() throws Exception {
        Response response = client.target(generateURL("/test.txt")).queryParam("query", "whosOnFirst").request().get();
        String entity = response.readEntity(String.class);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(entity, "plain: whosOnFirst");
    }

    /**
     * @tpTestDetails Map suffix .html to Accept: text/html
     * @tpSince RESTEasy 3.0.20
     */
    @Test
    @DisplayName("Test File Extension Mapping Html")
    public void testFileExtensionMappingHtml() throws Exception {
        Response response = client.target(generateURL("/test.html")).queryParam("query", "whosOnFirst").request().get();
        String entity = response.readEntity(String.class);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(entity, "html: whosOnFirst");
    }
}
