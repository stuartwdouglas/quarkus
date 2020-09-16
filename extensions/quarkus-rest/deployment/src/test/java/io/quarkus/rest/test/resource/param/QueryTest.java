package io.quarkus.rest.test.resource.param;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.resource.param.resource.QueryResource;
import io.quarkus.rest.test.resource.param.resource.QuerySearchQuery;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Parameters
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for @Query param of the resource, RESTEASY-715
 * @tpSince RESTEasy 3.0.16
 */
public class QueryTest {

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, QueryResource.class, QuerySearchQuery.class);
                }
            });

    @BeforeAll
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void cleanup() {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, QueryTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Use resource with @Query annotation with the parameter of custom type which consist of @QueryParam fields.
     *                Resteasy correctly parses the uri to get all specified parameters
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testQueryParamPrefix() throws Exception {
        WebTarget target = client.target(generateURL("/search?term=t1&order=ASC"));
        Response response = target.request().get();

        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals("term: 't1', order: 'ASC', limit: 'null'", response.readEntity(String.class));
    }
}
