package io.quarkus.rest.test.response;

import java.math.BigDecimal;
import java.util.function.Supplier;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NoContentException;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.response.resource.ProduceConsumeData;
import io.quarkus.rest.test.response.resource.ProduceConsumeResource;
import io.quarkus.rest.test.response.resource.ProduceConsumeTextData;
import io.quarkus.rest.test.response.resource.ProduceConsumeWildData;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class ProduceConsumeTest {

    static Client client;

    @BeforeClass
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterClass
    public static void cleanup() {
        client.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(ProduceConsumeData.class);
                    return TestUtil.finishContainerPrepare(war, null, ProduceConsumeResource.class,
                            ProduceConsumeWildData.class,
                            ProduceConsumeTextData.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ProduceConsumeTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Client sends GET request, server return empty successful response. Client parses the response
     *                and tries to read it as BigDecimal.class object.
     * @tpPassCrit Instance of NoContentException is thrown
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testEmpty() {
        Response response = client.target(generateURL("/resource/empty")).request().get();
        Assert.assertEquals(response.getStatus(), 200);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE,
                MediaType.TEXT_PLAIN_TYPE);
        try {
            BigDecimal big = response.readEntity(BigDecimal.class);
            Assert.fail();
        } catch (ProcessingException e) {
            Assert.assertTrue(e.getCause() instanceof NoContentException);
        }

    }

    /**
     * @tpTestDetails Client sends GET request, server return empty successful response. Client parses the response
     *                and tries to read it as Character.class object.
     * @tpPassCrit Instance of NoContentException is thrown
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testEmptyCharacter() {
        Response response = client.target(generateURL("/resource/empty")).request().get();
        Assert.assertEquals(response.getStatus(), 200);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE,
                MediaType.TEXT_PLAIN_TYPE);
        try {
            Character big = response.readEntity(Character.class);
            Assert.fail();
        } catch (ProcessingException e) {
            Assert.assertTrue(e.getCause() instanceof NoContentException);
        }

    }

    /**
     * @tpTestDetails Client sends GET request, server return empty successful response. Client parses the response
     *                and tries to read it as Integer.class object.
     * @tpPassCrit Instance of NoContentException is thrown
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testEmptyInteger() {
        Response response = client.target(generateURL("/resource/empty")).request().get();
        Assert.assertEquals(response.getStatus(), 200);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE,
                MediaType.TEXT_PLAIN_TYPE);
        try {
            Integer big = response.readEntity(Integer.class);
            Assert.fail();
        } catch (ProcessingException e) {
            Assert.assertTrue(e.getCause() instanceof NoContentException);
        }

    }

    /**
     * @tpTestDetails Client sends GET request, server return empty successful response. Client parses the response
     *                and tries to read it as MultivaluedMap.class object.
     * @tpPassCrit The returned MultivaluedMap object is null
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testEmptyForm() {
        Response response = client.target(generateURL("/resource/empty")).request().get();
        Assert.assertEquals(response.getStatus(), 200);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_FORM_URLENCODED);
        MultivaluedMap big = response.readEntity(MultivaluedMap.class);

        Assert.assertTrue(big == null || big.size() == 0);
    }

    /**
     * @tpTestDetails Client sends POST request with entity of mediatype WILDCARD. The application has two providers to
     *                write and read Data object. One for mediatype text/plain and one wildcard provider. The server choses one
     *                provider
     *                and sends response back to the client
     * @tpPassCrit The text/plain provider is chosen by the server, because if the request has wildcard mediatype,
     *             the most specific provider has to be chosen.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testWild() {
        client.register(ProduceConsumeTextData.class);
        client.register(ProduceConsumeWildData.class);
        Response response = client.target(generateURL("/resource/wild")).request("*/*")
                .post(Entity.entity("data", MediaType.WILDCARD_TYPE));
        Assert.assertEquals(response.getStatus(), 200);
        ProduceConsumeData data = response.readEntity(ProduceConsumeData.class);
        Assert.assertEquals("Data{data='data:text:text', type='text'}", data.toString());
        response.close();
    }

}
