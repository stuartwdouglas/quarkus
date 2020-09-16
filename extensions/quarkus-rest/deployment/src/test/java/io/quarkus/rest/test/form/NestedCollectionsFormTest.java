package io.quarkus.rest.test.form;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.form.resource.NestedCollectionsFormAddress;
import io.quarkus.rest.test.form.resource.NestedCollectionsFormCountry;
import io.quarkus.rest.test.form.resource.NestedCollectionsFormPerson;
import io.quarkus.rest.test.form.resource.NestedCollectionsFormResource;
import io.quarkus.rest.test.form.resource.NestedCollectionsFormTelephoneNumber;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resources
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test nested form parameters
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Nested Collections Form Test")
public class NestedCollectionsFormTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(NestedCollectionsFormTelephoneNumber.class);
            war.addClass(NestedCollectionsFormPerson.class);
            war.addClass(NestedCollectionsFormCountry.class);
            war.addClass(NestedCollectionsFormAddress.class);
            return TestUtil.finishContainerPrepare(war, null, NestedCollectionsFormResource.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, NestedCollectionsFormTest.class.getSimpleName());
    }

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void close() {
        client.close();
    }

    /**
     * @tpTestDetails Set all relevant parameters to form.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Should Support Collections With Nested Objects In Form")
    public void shouldSupportCollectionsWithNestedObjectsInForm() throws Exception {
        javax.ws.rs.core.Form form = new javax.ws.rs.core.Form().param("telephoneNumbers[0].country.code", "31")
                .param("telephoneNumbers[0].number", "0612345678").param("telephoneNumbers[1].country.code", "91")
                .param("telephoneNumbers[1].number", "9717738723").param("address[INVOICE].street", "Main Street")
                .param("address[INVOICE].houseNumber", "2").param("address[INVOICE].country.code", "NL")
                .param("address[SHIPPING].street", "Square One").param("address[SHIPPING].houseNumber", "13")
                .param("address[SHIPPING].country.code", "IN");
        WebTarget base = client
                .target(PortProviderUtil.generateURL("/person", NestedCollectionsFormTest.class.getSimpleName()));
        Response response = base.request().accept(MediaType.TEXT_PLAIN).post(Entity.form(form));
        Assertions.assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        response.close();
    }
}
