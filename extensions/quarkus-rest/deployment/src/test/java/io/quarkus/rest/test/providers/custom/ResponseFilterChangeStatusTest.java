package io.quarkus.rest.test.providers.custom;

import java.util.function.Supplier;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.rules.ExpectedException;

import io.quarkus.rest.test.providers.custom.resource.ResponseFilterChangeStatusResource;
import io.quarkus.rest.test.providers.custom.resource.ResponseFilterChangeStatusResponseFilter;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class ResponseFilterChangeStatusTest {

    protected static final Logger logger = LogManager.getLogger(ResponseFilterChangeStatusTest.class.getName());

    @Rule
    public ExpectedException thrown = ExpectedException.none();

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

                    return TestUtil.finishContainerPrepare(war, null, ResponseFilterChangeStatusResource.class,
                            ResponseFilterChangeStatusResponseFilter.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ResponseFilterChangeStatusTest.class.getSimpleName());
    }

    @AfterClass
    public static void close() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Client sends HEAD request. The response gets processed by custom ResponseFilter.
     * @tpPassCrit The response code status is changed to 201 (CREATED), the response doesn't contain any entity,
     *             because this was HEAD request and response has set up its MediaType
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testDefaultHead() {
        Response response = client.target(generateURL("/default_head")).request().head();
        Assert.assertEquals(Status.CREATED, response.getStatus());

        thrown.expect(ProcessingException.class);
        response.readEntity(String.class);

        logger.info(response.getMediaType());
        Assert.assertTrue("Response must heave set up all headers, as if GET request was called.",
                response.getMediaType().equals(MediaType.TEXT_PLAIN_TYPE));
        response.close();
    }

    /**
     * @tpTestDetails Client sends POST request. The response gets processed by custom ResponseFilter.
     * @tpPassCrit The response code status is changed to 201 (CREATED)
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testChangeStatus() {
        Response response = client.target(generateURL("/empty")).request().post(null);
        Assert.assertEquals(Status.CREATED, response.getStatus());
        response.close();
    }
}
