package io.quarkus.rest.test.providers.custom;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.providers.custom.resource.ResponseContainerResource;
import io.quarkus.rest.test.providers.custom.resource.ResponseContainerResponseFilter;
import io.quarkus.rest.test.providers.custom.resource.ResponseContainerSecondResponseFilter;
import io.quarkus.rest.test.providers.custom.resource.ResponseContainerTemplateFilter;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class ResponseContainerFilterTest {

    protected static final Logger logger = Logger.getLogger(ResponseContainerFilterTest.class.getName());

    static Client client;

    @BeforeClass
    public static void setup() throws Exception {
        client = ClientBuilder.newClient();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(ResponseContainerTemplateFilter.class);
                    return TestUtil.finishContainerPrepare(war, null, ResponseContainerResource.class,
                            ResponseContainerResponseFilter.class, ResponseContainerSecondResponseFilter.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ResponseContainerFilterTest.class.getSimpleName());
    }

    @AfterClass
    public static void close() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Client sends POST request with it's custom header "OPERATION" specified in it. Server has registered
     *                two ContainerResponseFilters, which have common ancestor and different priority. The filter ResponseFilter
     *                with higher priority should be used here first, because the order of execution for Response filters is
     *                descending.
     * @tpPassCrit The ResponseFilter is used first for processing the response
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testHasEntity() {
        Response response = client.target(generateURL("/resource/hasentity")).request("*/*")
                .header("OPERATION", "hasentity").post(Entity.entity("entity", MediaType.WILDCARD_TYPE));
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals("The ResponseFilters were used in different order than expected", MediaType.TEXT_PLAIN_TYPE,
                response.getMediaType());
        logger.info(response.readEntity(String.class));
        response.close();

    }

}
