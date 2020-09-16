package io.quarkus.rest.test.validation;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.jboss.resteasy.api.validation.Validation;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.rest.test.validation.resource.ValidationCoreFooReaderWriter;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Response
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test ignored validation
 * @tpSince RESTEasy 3.0.16
 */
public class GetterReturnValueNotValidatedTest {
    protected final Logger logger = Logger.getLogger(GetterReturnValueNotValidatedTest.class.getName());
    QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, (Class<?>[]) null);
                }
            });

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, GetterReturnValueNotValidatedTest.class.getSimpleName());
    }

    @Before
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient().register(ValidationCoreFooReaderWriter.class);
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Validation of getter return value is not expected.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testReturnValues() throws Exception {
        Response response = client.target(generateURL("/get")).request().get();
        response.close();

        response = client.target(generateURL("/set")).request().get();
        Assert.assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        response.close();

        // Valid native constraint
        response = client.target(generateURL("/get")).request().get();
        String entity = response.readEntity(String.class);
        logger.info(String.format("Response: %s", entity.replace('\r', ' ').replace('\t', ' ').replace('\n', ' ')));
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String header = response.getHeaderString(Validation.VALIDATION_HEADER);
        Assert.assertNull("Validation header was not excepted", header);
        Assert.assertEquals("Wrong content of response", "a", entity);
        response.close();
    }
}
