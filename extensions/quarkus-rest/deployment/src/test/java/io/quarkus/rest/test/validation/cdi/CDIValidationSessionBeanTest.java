package io.quarkus.rest.test.validation.cdi;

import static org.junit.Assert.assertEquals;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.api.validation.ResteasyConstraintViolation;
import org.jboss.resteasy.api.validation.ResteasyViolationException;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.jboss.resteasy.plugins.validation.ResteasyViolationExceptionImpl;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.rest.test.validation.cdi.resource.CDIValidationSessionBeanResource;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Response
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1008
 * @tpSince RESTEasy 3.0.16
 */
public class CDIValidationSessionBeanTest {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, CDIValidationSessionBeanResource.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, CDIValidationSessionBeanTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Check for invalid parameter
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testInvalidParam() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        Invocation.Builder request = client.target(generateURL("/test/resource/0")).request();
        ClientResponse response = (ClientResponse) request.get();
        String answer = response.readEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        ResteasyViolationException e = new ResteasyViolationExceptionImpl(String.class.cast(answer));
        TestUtil.countViolations(e, 1, 0, 0, 1, 0);
        ResteasyConstraintViolation cv = e.getParameterViolations().iterator().next();
        Assert.assertTrue("Expected validation error is not in response",
                cv.getMessage().equals("must be greater than or equal to 7"));
        client.close();
    }
}
