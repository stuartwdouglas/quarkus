package io.quarkus.rest.test.resource.request;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
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
public class DateFormatPathTest {
    static Client client;

    @BeforeClass
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
    }

    @AfterClass
    public static void close() {
        client.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
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
    public void testDate() throws Exception {
        Response response = client.target(generateURL("/widget/08%2F26%2F2009")).request().get();
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("08/26/2009", response.readEntity(String.class));
        response.close();
    }
}
