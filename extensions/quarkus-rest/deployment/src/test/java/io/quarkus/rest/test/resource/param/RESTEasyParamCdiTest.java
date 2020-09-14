package io.quarkus.rest.test.resource.param;

import static org.hamcrest.CoreMatchers.is;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.resource.param.resource.RESTEasyParamBeanCdi;
import io.quarkus.rest.test.resource.param.resource.RESTEasyParamCdiResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Parameters
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for RESTEasy param annotations (https://issues.jboss.org/browse/RESTEASY-1880)
 *                    Test logic is in the end-point in deployment.
 *                    This test checks CDI integration.
 * @tpSince RESTEasy 3.6
 */
public class RESTEasyParamCdiTest {
    protected static final Logger logger = Logger.getLogger(RESTEasyParamCdiTest.class.getName());

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, RESTEasyParamCdiResource.class,
                            RESTEasyParamBeanCdi.class);
                }
            });

    @Before
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, RESTEasyParamCdiTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Checks end-point with @RequestScoped annotation
     * @tpSince RESTEasy 3.6
     */
    @Test
    public void requestScopedTest() throws Exception {
        for (Integer i = 0; i < 100; i++) {
            logger.info("Request " + i);
            String defaultValue = i.toString();
            Response response = client.target(generateURL(String.format("/%d/%d/%d/%d",
                    i, i, i, i)))
                    .queryParam("queryParam0", defaultValue)
                    .queryParam("queryParam1", defaultValue)
                    .queryParam("queryParam2", defaultValue)
                    .queryParam("queryParam3", defaultValue)
                    .matrixParam("matrixParam0", defaultValue)
                    .matrixParam("matrixParam1", defaultValue)
                    .matrixParam("matrixParam2", defaultValue)
                    .matrixParam("matrixParam3", defaultValue)
                    .request()
                    .header("headerParam0", defaultValue)
                    .header("headerParam1", defaultValue)
                    .header("headerParam2", defaultValue)
                    .header("headerParam3", defaultValue)
                    .cookie("cookieParam0", defaultValue)
                    .cookie("cookieParam1", defaultValue)
                    .cookie("cookieParam2", defaultValue)
                    .cookie("cookieParam3", defaultValue)
                    .post(Entity.form(new Form()
                            .param("formParam0", defaultValue)
                            .param("formParam1", defaultValue)
                            .param("formParam2", defaultValue)
                            .param("formParam3", defaultValue)));
            Assert.assertThat("expected response code is 200, get: " + response.getStatus(),
                    response.getStatus(), is(200));
            String message = response.readEntity(String.class);
            Assert.assertThat("expected value: " + defaultValue + ", get: " + message, message, is(defaultValue));
        }
    }

    /**
     * @tpTestDetails Checks end-point with a BeanParam decorated by @RequestScoped annotation
     * @tpSince RESTEasy 3.6
     */
    @Test
    public void requestScopedBeanParamTest() throws Exception {
        for (Integer i = 0; i < 3; i++) {
            logger.info("Request " + i);
            String defaultValue = i.toString();
            Response response = client.target(generateURL(String.format("/%d/%d/%d/%d/%d",
                    i, i, i, i, i)))
                    .queryParam("queryParam0", defaultValue)
                    .queryParam("queryParam1", defaultValue)
                    .queryParam("queryParam2", defaultValue)
                    .queryParam("queryParam3", defaultValue)
                    .matrixParam("matrixParam0", defaultValue)
                    .matrixParam("matrixParam1", defaultValue)
                    .matrixParam("matrixParam2", defaultValue)
                    .matrixParam("matrixParam3", defaultValue)
                    .request()
                    .header("headerParam0", defaultValue)
                    .header("headerParam1", defaultValue)
                    .header("headerParam2", defaultValue)
                    .header("headerParam3", defaultValue)
                    .cookie("cookieParam0", defaultValue)
                    .cookie("cookieParam1", defaultValue)
                    .cookie("cookieParam2", defaultValue)
                    .cookie("cookieParam3", defaultValue)
                    .post(Entity.form(new Form()
                            .param("formParam0", defaultValue)
                            .param("formParam1", defaultValue)
                            .param("formParam2", defaultValue)
                            .param("formParam3", defaultValue)));
            Assert.assertThat("expected response code is 200, get: " + response.getStatus(),
                    response.getStatus(), is(200));
            String message = response.readEntity(String.class);
            Assert.assertThat("expected value: " + defaultValue + ", get: " + message, message, is(defaultValue));
        }
    }
}
