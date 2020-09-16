package io.quarkus.rest.test.validation.cdi;

import static org.junit.Assert.assertEquals;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.api.validation.ViolationReport;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.rest.test.validation.cdi.resource.SubresourceValidationResource;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Response
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1103
 * @tpSince RESTEasy 3.0.16
 */
public class SubresourceValidationTest {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, SubresourceValidationResource.class);
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
        return PortProviderUtil.generateURL(path, SubresourceValidationTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test for subresources
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testSubresource() throws Exception {
        Invocation.Builder request = client.target(generateURL("/sub/17?limit=abcdef")).request();
        ClientResponse response = (ClientResponse) request.get();
        ViolationReport r = new ViolationReport(response.readEntity(String.class));
        TestUtil.countViolations(r, 0, 0, 2, 0);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    /**
     * @tpTestDetails Test for validation of returned value
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testReturnValue() throws Exception {
        Invocation.Builder request = client.target(generateURL("/sub/return/abcd")).request();
        ClientResponse response = (ClientResponse) request.get();
        ViolationReport r = new ViolationReport(response.readEntity(String.class));
        TestUtil.countViolations(r, 0, 0, 0, 1);
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }
}
