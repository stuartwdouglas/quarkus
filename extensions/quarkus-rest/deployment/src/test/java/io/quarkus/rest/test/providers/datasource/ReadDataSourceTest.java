package io.quarkus.rest.test.providers.datasource;

import java.io.UnsupportedEncodingException;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.entity.StringEntity;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.datasource.resource.ReadDataSourceResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter DataSource provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Read Data Source Test")
public class ReadDataSourceTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, ReadDataSourceResource.class);
        }
    });

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ReadDataSourceTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Tests DataSourceProviders ability to read the same stream twice and verifies the results of both reads
     *                are equal
     * @tpInfo RESTEASY-1182
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Data Source Input Stream")
    public void testDataSourceInputStream() throws Exception {
        WebTarget target = client.target(generateURL("/" + ReadDataSourceResource.PATH_UPLOAD));
        Response response = target.request().post(Entity.entity(createContent(), "text/plain"));
        final StringBuilder msg = new StringBuilder();
        final String entity = response.readEntity(String.class);
        if (entity != null) {
            msg.append("\n").append(entity);
        }
        Assertions.assertEquals("Unexpected response: " + msg.toString(), Status.OK.getStatusCode(), response.getStatus());
    }

    private StringEntity createContent() throws UnsupportedEncodingException {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 2500; i++) {
            stringBuilder.append(i).append(":\n");
        }
        return new StringEntity(stringBuilder.toString());
    }
}
