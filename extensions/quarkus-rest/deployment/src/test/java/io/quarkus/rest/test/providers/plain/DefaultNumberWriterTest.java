package io.quarkus.rest.test.providers.plain;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.providers.plain.resource.DefaultNumberWriterCustom;
import io.quarkus.rest.test.providers.plain.resource.DefaultNumberWriterResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Plain provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for DefaultNumberWriter provider.
 *                    Regression test for partial fix for JBEAP-2847.
 * @tpSince RESTEasy 3.0.16
 */

public class DefaultNumberWriterTest {
    private static Logger logger = Logger.getLogger(DefaultNumberWriterTest.class);
    private static final String WRONG_RESPONSE_ERROR_MSG = "Response contains wrong response";
    private static final String WRONG_PROVIDER_USED_ERROR_MSG = "Wrong provider was used";

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(TestUtil.class, PortProviderUtil.class);
                    // Arquillian in the deployment

                    return TestUtil.finishContainerPrepare(war, null, DefaultNumberWriterResource.class,
                            DefaultNumberWriterCustom.class);
                }
            });

    protected Client client;

    @Before
    public void beforeTest() {
        client = ClientBuilder.newClient();
    }

    @After
    public void afterTest() {
        client.close();
        client = null;
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, DefaultNumberWriterTest.class.getSimpleName());
    }

    @After
    public void resetProviderFlag() {
        DefaultNumberWriterCustom.used = false;
    }

    /**
     * @tpTestDetails Tests Byte object
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testByte() throws Exception {
        Response response = client.target(generateURL("/test/Byte")).request().get();
        response.bufferEntity();
        logger.info(response.readEntity(String.class));
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals(WRONG_RESPONSE_ERROR_MSG, "123", response.getEntity());
        Assert.assertTrue(WRONG_PROVIDER_USED_ERROR_MSG, DefaultNumberWriterCustom.used);
    }

    /**
     * @tpTestDetails Tests byte primitive
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testBytePrimitive() throws Exception {
        Response response = client.target(generateURL("/test/byte")).request().get();
        response.bufferEntity();
        logger.info(response.readEntity(String.class));
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals(WRONG_RESPONSE_ERROR_MSG, "123", response.getEntity());
        Assert.assertTrue(WRONG_PROVIDER_USED_ERROR_MSG, DefaultNumberWriterCustom.used);
    }

    /**
     * @tpTestDetails Tests Double object
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testDouble() throws Exception {
        Response response = client.target(generateURL("/test/Double")).request().get();
        response.bufferEntity();
        logger.info(response.readEntity(String.class));
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals(WRONG_RESPONSE_ERROR_MSG, "123.4", response.getEntity());
        Assert.assertTrue(WRONG_PROVIDER_USED_ERROR_MSG, DefaultNumberWriterCustom.used);
    }

    /**
     * @tpTestDetails Tests double primitive
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testDoublePrimitive() throws Exception {
        Response response = client.target(generateURL("/test/double")).request().get();
        response.bufferEntity();
        logger.info(response.readEntity(String.class));
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals(WRONG_RESPONSE_ERROR_MSG, "123.4", response.getEntity());
        Assert.assertTrue(WRONG_PROVIDER_USED_ERROR_MSG, DefaultNumberWriterCustom.used);
    }

    /**
     * @tpTestDetails Tests Float object
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testFloat() throws Exception {
        Response response = client.target(generateURL("/test/Float")).request().get();
        response.bufferEntity();
        logger.info(response.readEntity(String.class));
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals(WRONG_RESPONSE_ERROR_MSG, "123.4", response.getEntity());
        Assert.assertTrue(WRONG_PROVIDER_USED_ERROR_MSG, DefaultNumberWriterCustom.used);
    }

    /**
     * @tpTestDetails Tests float primitive
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testFloatPrimitive() throws Exception {
        Response response = client.target(generateURL("/test/float")).request().get();
        response.bufferEntity();
        logger.info(response.readEntity(String.class));
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals(WRONG_RESPONSE_ERROR_MSG, "123.4", response.getEntity());
        Assert.assertTrue(WRONG_PROVIDER_USED_ERROR_MSG, DefaultNumberWriterCustom.used);
    }

    /**
     * @tpTestDetails Tests Integer object
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testInteger() throws Exception {
        Response response = client.target(generateURL("/test/Integer")).request().get();
        response.bufferEntity();
        logger.info(response.readEntity(String.class));
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals(WRONG_RESPONSE_ERROR_MSG, "123", response.getEntity());
        Assert.assertTrue(WRONG_PROVIDER_USED_ERROR_MSG, DefaultNumberWriterCustom.used);
    }

    /**
     * @tpTestDetails Tests integer primitive
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testIntegerPrimitive() throws Exception {
        Response response = client.target(generateURL("/test/integer")).request().get();
        response.bufferEntity();
        logger.info(response.readEntity(String.class));
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals(WRONG_RESPONSE_ERROR_MSG, "123", response.getEntity());
        Assert.assertTrue(WRONG_PROVIDER_USED_ERROR_MSG, DefaultNumberWriterCustom.used);
    }

    /**
     * @tpTestDetails Tests Long object
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testLong() throws Exception {
        Response response = client.target(generateURL("/test/Long")).request().get();
        response.bufferEntity();
        logger.info(response.readEntity(String.class));
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals(WRONG_RESPONSE_ERROR_MSG, "123", response.getEntity());
        Assert.assertTrue(WRONG_PROVIDER_USED_ERROR_MSG, DefaultNumberWriterCustom.used);
    }

    /**
     * @tpTestDetails Tests long primitive
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testLongPrimitive() throws Exception {
        Response response = client.target(generateURL("/test/long")).request().get();
        response.bufferEntity();
        logger.info(response.readEntity(String.class));
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals(WRONG_RESPONSE_ERROR_MSG, "123", response.getEntity());
        Assert.assertTrue(WRONG_PROVIDER_USED_ERROR_MSG, DefaultNumberWriterCustom.used);
    }

    /**
     * @tpTestDetails Tests Short object
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testShort() throws Exception {
        Response response = client.target(generateURL("/test/Short")).request().get();
        response.bufferEntity();
        logger.info(response.readEntity(String.class));
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals(WRONG_RESPONSE_ERROR_MSG, "123", response.getEntity());
        Assert.assertTrue(WRONG_PROVIDER_USED_ERROR_MSG, DefaultNumberWriterCustom.used);
    }

    /**
     * @tpTestDetails Tests short primitive
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testShortPrimitive() throws Exception {
        Response response = client.target(generateURL("/test/short")).request().get();
        response.bufferEntity();
        logger.info(response.readEntity(String.class));
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals(WRONG_RESPONSE_ERROR_MSG, "123", response.getEntity());
        Assert.assertTrue(WRONG_PROVIDER_USED_ERROR_MSG, DefaultNumberWriterCustom.used);
    }

    /**
     * @tpTestDetails Tests BigDecimal object
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testBigDecimal() throws Exception {
        Response response = client.target(generateURL("/test/bigDecimal")).request().get();
        response.bufferEntity();
        logger.info(response.readEntity(String.class));
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals(WRONG_RESPONSE_ERROR_MSG, "123", response.getEntity());
        Assert.assertTrue(WRONG_PROVIDER_USED_ERROR_MSG, DefaultNumberWriterCustom.used);
    }

    /**
     * @tpTestDetails Tests BigDecimal object with register custom provider on client
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testProviderGetsUsed() throws Exception {
        client.register(DefaultNumberWriterCustom.class);
        Response response = client.target(generateURL("/test/bigDecimal")).request().get();
        response.bufferEntity();
        logger.info(response.readEntity(String.class));
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals(WRONG_RESPONSE_ERROR_MSG, "123", response.getEntity());
        Assert.assertTrue(WRONG_PROVIDER_USED_ERROR_MSG, DefaultNumberWriterCustom.used);
    }
}
