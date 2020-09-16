package io.quarkus.rest.test.resource.param;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.Assert;
import io.quarkus.rest.test.resource.param.resource.FormParamBasicResource;
import io.quarkus.rest.test.resource.param.resource.FormParamEntityPrototype;
import io.quarkus.rest.test.resource.param.resource.FormParamEntityThrowsIllegaArgumentException;
import io.quarkus.rest.test.resource.param.resource.FormParamEntityWithConstructor;
import io.quarkus.rest.test.resource.param.resource.FormParamEntityWithFromString;
import io.quarkus.rest.test.resource.param.resource.FormParamResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Parameters
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for form parameters
 * @tpSince RESTEasy 3.0.16
 */
public class FormParamTest {
    static Client client;

    private static final String ERROR_CODE = "Wrong response";

    @BeforeAll
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(FormParamEntityPrototype.class);
                    war.addClass(FormParamEntityThrowsIllegaArgumentException.class);
                    war.addClass(FormParamEntityWithConstructor.class);
                    war.addClass(FormParamEntityWithFromString.class);
                    return TestUtil.finishContainerPrepare(war, null, FormParamResource.class,
                            FormParamBasicResource.class);
                }
            });

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, FormParamTest.class.getSimpleName());
    }

    @AfterAll
    public static void cleanup() {
        client.close();
    }

    private static final String SENT = "_`'$X Y@\"a a\"";
    private static final String ENCODED = "_%60%27%24X+Y%40%22a+a%22";

    /**
     * @tpTestDetails Check form parameters with POST method
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void postTest() {
        Entity entity = Entity.entity("param=" + ENCODED, MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response response = client.target(generateURL("/form")).request().post(entity);
        Assert.assertEquals(response.getStatus(), Status.OK.getStatusCode());
        Assert.assertEquals(ERROR_CODE, response.readEntity(String.class), ENCODED);
        response.close();
    }

    /**
     * @tpTestDetails Check non default form parameters, accept special object
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void nonDefaultFormParamFromStringTest() {
        Entity entity = Entity.entity("default_argument=" + SENT, MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response response = client.target(generateURL("/FormParamTest/ParamEntityWithFromString")).request().post(entity);
        Assert.assertEquals(response.getStatus(), Status.OK.getStatusCode());
        Assert.assertEquals(ERROR_CODE, "CTS_FORMPARAM:" + ENCODED, response.readEntity(String.class));
        response.close();
    }

    /**
     * @tpTestDetails Check non default form parameters, accept String
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void string() {
        Entity entity = Entity.entity("default_argument=" + SENT, MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response response = client.target(generateURL("/FormParamTest/string")).request().post(entity);
        Assert.assertEquals(response.getStatus(), Status.OK.getStatusCode());
        Assert.assertEquals(ERROR_CODE, response.readEntity(String.class), "CTS_FORMPARAM:" + ENCODED);
        response.close();
    }

    /**
     * @tpTestDetails Check non default form parameters, accept sorted set
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void defaultFormParamFromSortedSetFromStringTest() {
        Response response = client.target(generateURL("/FormParamTest/SortedSetFromString")).request()
                .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED).post(null);
        Assert.assertEquals(response.getStatus(), Status.OK.getStatusCode());
        Assert.assertEquals(ERROR_CODE, "CTS_FORMPARAM:SortedSetFromString", response.readEntity(String.class));
        response.close();
    }

    /**
     * @tpTestDetails Check non default form parameters, accept list
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void defaultListConstructor() {
        Response response = client.target(generateURL("/FormParamTest/ListConstructor")).request()
                .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED).post(null);
        Assert.assertEquals(response.getStatus(), Status.OK.getStatusCode());
        Assert.assertEquals(ERROR_CODE, "CTS_FORMPARAM:ListConstructor", response.readEntity(String.class));
        response.close();
    }

    /**
     * @tpTestDetails Check wrong arguments, exception is excepted
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testIllegalArgumentException() {
        Response response = client.target(generateURL("/FormParamTest/IllegalArgumentException")).request()
                .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED).post(null);
        Assert.assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
        Assert.assertTrue(ERROR_CODE, response.readEntity(String.class).startsWith("RESTEASY003870:"));
        response.close();
    }

}
