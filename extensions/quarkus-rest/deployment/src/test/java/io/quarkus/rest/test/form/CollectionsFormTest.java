package io.quarkus.rest.test.form;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.form.resource.CollectionsFormAddress;
import io.quarkus.rest.test.form.resource.CollectionsFormPerson;
import io.quarkus.rest.test.form.resource.CollectionsFormResource;
import io.quarkus.rest.test.form.resource.CollectionsFormTelephoneNumber;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Form tests
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test inner form parameters and collections.
 * @tpSince RESTEasy 3.0.16
 */
public class CollectionsFormTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(CollectionsFormPerson.class, CollectionsFormTelephoneNumber.class,
                            CollectionsFormAddress.class);
                    return TestUtil.finishContainerPrepare(war, null, CollectionsFormResource.class);
                }
            });

    /**
     * @tpTestDetails Set all relevant parameters to form.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void shouldSupportCollectionsInForm() throws Exception {
        javax.ws.rs.core.Form form = new javax.ws.rs.core.Form()
                .param("telephoneNumbers[0].countryCode", "31")
                .param("telephoneNumbers[0].number", "0612345678")
                .param("telephoneNumbers[1].countryCode", "91")
                .param("telephoneNumbers[1].number", "9717738723")
                .param("address[INVOICE].street", "Main Street")
                .param("address[INVOICE].houseNumber", "2")
                .param("address[SHIPPING].street", "Square One")
                .param("address[SHIPPING].houseNumber", "13");

        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        WebTarget base = client.target(PortProviderUtil.generateURL("/person", CollectionsFormTest.class.getSimpleName()));
        Response response = base.request().accept(MediaType.TEXT_PLAIN).post(Entity.form(form));

        Assert.assertEquals(Status.NO_CONTENT, response.getStatus());
        client.close();
    }
}
