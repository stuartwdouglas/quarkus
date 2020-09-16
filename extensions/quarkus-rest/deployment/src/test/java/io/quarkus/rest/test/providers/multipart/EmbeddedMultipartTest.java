package io.quarkus.rest.test.providers.multipart;

import java.util.function.Supplier;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.plugins.providers.multipart.MultipartOutput;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.providers.multipart.resource.EmbeddedMultipartCustomer;
import io.quarkus.rest.test.providers.multipart.resource.EmbeddedMultipartResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Multipart provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-929
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Embedded Multipart Test")
public class EmbeddedMultipartTest {

    protected static final MediaType MULTIPART_MIXED = new MediaType("multipart", "mixed");

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(EmbeddedMultipartCustomer.class);
            return TestUtil.finishContainerPrepare(war, null, EmbeddedMultipartResource.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, EmbeddedMultipartTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test embedded part of multipart message
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Embedded")
    public void testEmbedded() {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        QuarkusRestWebTarget target = client.target(generateURL("/embedded"));
        EmbeddedMultipartCustomer customer = new EmbeddedMultipartCustomer("bill");
        MultipartOutput innerPart = new MultipartOutput();
        innerPart.addPart(customer, MediaType.APPLICATION_XML_TYPE);
        MultipartOutput outerPart = new MultipartOutput();
        outerPart.addPart(innerPart, MULTIPART_MIXED);
        Entity<MultipartOutput> entity = Entity.entity(outerPart, MULTIPART_MIXED);
        String response = target.request().post(entity, String.class);
        Assertions.assertEquals("bill", response, "Wrong content of response");
        client.close();
    }

    /**
     * @tpTestDetails Test complete multipart message
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Customer")
    public void testCustomer() {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        QuarkusRestWebTarget target = client.target(generateURL("/customer"));
        EmbeddedMultipartCustomer customer = new EmbeddedMultipartCustomer("bill");
        MultipartOutput outerPart = new MultipartOutput();
        outerPart.addPart(customer, MediaType.APPLICATION_XML_TYPE);
        Entity<MultipartOutput> entity = Entity.entity(outerPart, MULTIPART_MIXED);
        String response = target.request().post(entity, String.class);
        Assertions.assertEquals("bill", response, "Wrong content of response");
        client.close();
    }

    /**
     * @tpTestDetails Test exception in embedded multipart message
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Invalid")
    public void testInvalid() {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        try {
            QuarkusRestWebTarget target = client.target(generateURL("/invalid"));
            EmbeddedMultipartCustomer customer = new EmbeddedMultipartCustomer("bill");
            MultipartOutput outerPart = new MultipartOutput();
            outerPart.addPart(customer, MediaType.APPLICATION_XML_TYPE);
            Entity<MultipartOutput> entity = Entity.entity(outerPart, MULTIPART_MIXED);
            target.request().post(entity, String.class);
            Assertions.fail("Exception is expected");
        } catch (InternalServerErrorException e) {
            Response response = e.getResponse();
            Assertions.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus(),
                    "Wrong type of exception");
        } finally {
            client.close();
        }
    }
}
