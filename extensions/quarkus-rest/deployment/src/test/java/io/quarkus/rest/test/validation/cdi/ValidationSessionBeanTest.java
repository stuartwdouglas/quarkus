package io.quarkus.rest.test.validation.cdi;

import static org.junit.Assert.assertEquals;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.api.validation.ResteasyConstraintViolation;
import org.jboss.resteasy.api.validation.ResteasyViolationException;
import org.jboss.resteasy.plugins.validation.ResteasyViolationExceptionImpl;
import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Response
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-923
 * @tpSince RESTEasy 3.0.16
 */
@Ignore("RESTEASY-2601") //FIXME
public class ValidationSessionBeanTest {
    @SuppressWarnings(value = "unchecked")
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

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ValidationSessionBeanTest.class.getSimpleName());
    }

    @Test
    public void testInvalidParam() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        Response response = client.target(generateURL("/test/resource")).queryParam("param", "abc").request().get();
        String answer = response.readEntity(String.class);
        assertEquals(Status.BAD_REQUEST, response.getStatus());
        ResteasyViolationException e = new ResteasyViolationExceptionImpl(String.class.cast(answer));
        int c = e.getViolations().size();
        Assert.assertTrue(c == 1 || c == 2);
        TestUtil.countViolations(e, c, 0, 0, c, 0);
        ResteasyConstraintViolation cv = e.getParameterViolations().iterator().next();
        Assert.assertTrue("Expected validation error is not in response",
                cv.getMessage().startsWith("size must be between 4 and"));
        Assert.assertTrue("Expected validation error is not in response", answer.contains("size must be between 4 and"));
        response.close();
        client.close();
    }
}
