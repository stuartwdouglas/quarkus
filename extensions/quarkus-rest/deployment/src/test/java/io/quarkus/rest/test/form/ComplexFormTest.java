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
import io.quarkus.rest.test.form.resource.ComplexFormAddress;
import io.quarkus.rest.test.form.resource.ComplexFormPerson;
import io.quarkus.rest.test.form.resource.ComplexFormResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Form tests
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test complex inner form parameters. Check return value, it is based on form.
 * @tpSince RESTEasy 3.0.16
 */
public class ComplexFormTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(ComplexFormPerson.class, ComplexFormAddress.class);
                    return TestUtil.finishContainerPrepare(war, null, ComplexFormResource.class);
                }
            });

    /**
     * @tpTestDetails Set all relevant parameters to form and check return value.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void shouldSupportNestedForm() throws Exception {
        javax.ws.rs.core.Form form = new javax.ws.rs.core.Form()
                .param("name", "John Doe")
                .param("invoice.street", "Main Street")
                .param("shipping.street", "Station Street");

        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        WebTarget base = client.target(PortProviderUtil.generateURL("/person", CollectionsFormTest.class.getSimpleName()));
        Response response = base.request().accept(MediaType.TEXT_PLAIN).post(Entity.form(form));

        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("Wrong content of response", "name:'John Doe', invoice:'Main Street', shipping:'Station Street'",
                response.readEntity(String.class));
        client.close();
    }
}
