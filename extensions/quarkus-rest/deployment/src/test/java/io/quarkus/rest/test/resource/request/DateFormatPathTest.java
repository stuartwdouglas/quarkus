package io.quarkus.rest.test.resource.request;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
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

import io.quarkus.rest.test.resource.request.resource.DateFormatPathResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpTestCaseDetails Tests date encoding as query parameter
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Date Format Path Test")
public class DateFormatPathTest {

    static Client client;

    @BeforeAll
    public static void before() throws Exception {
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
            return TestUtil.finishContainerPrepare(war, null, DateFormatPathResource.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, DateFormatPathTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test date 08/26/2009
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Date")
    public void testDate() throws Exception {
        Response response = client.target(generateURL("/widget/08%2F26%2F2009")).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "08/26/2009");
        response.close();
    }
}
