package io.quarkus.rest.test.client;

import java.util.function.Supplier;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.ProxyBuilder;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.client.resource.ClientFormResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpSince RESTEasy 3.0.16
 */
public class ClientFormParamTest extends ClientTestBase {

    static QuarkusRestClient client;

    @Path("/form")
    public interface ClientFormResourceInterface {
        @POST
        String put(@FormParam("value") String value);

        @POST
        @Path("object")
        @Produces(MediaType.APPLICATION_FORM_URLENCODED)
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        Form post(Form form);
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(ClientFormParamTest.class);
                    war.addClass(ClientTestBase.class);
                    return TestUtil.finishContainerPrepare(war, null, ClientFormResource.class);
                }
            });

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Client sends POST request with Form entity with one parameter.
     * @tpPassCrit The resulting Form entity contains the original parameter
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testClientFormOneParameter() throws Exception {

        final ClientFormResourceInterface proxy = ProxyBuilder.builder(ClientFormResourceInterface.class,
                client.target(generateURL(""))).build();
        String result = proxy.put("value");
        Assert.assertEquals("The result doesn't match the expected one", result, "value");
        result = client.target(generateURL("/form")).request().post(Entity.form(new Form().param("value", "value")),
                String.class);
        Assert.assertEquals("The result doesn't match the expected on, when using Form parameter", result, "value");
    }

    /**
     * @tpTestDetails Client sends POST request with Form entity with two parameters.
     * @tpPassCrit The resulting Form entity contains both original parameters
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testClientFormTwoParameters() throws Exception {
        final ClientFormResourceInterface proxy = ProxyBuilder.builder(ClientFormResourceInterface.class,
                client.target(generateURL(""))).build();
        Form form = new Form().param("bill", "burke").param("foo", "bar");
        Form resultForm = proxy.post(form);
        Assert.assertEquals("The form map size on the response doesn't match the original", resultForm.asMap().size(),
                form.asMap().size());
        Assert.assertEquals("The resulting form doesn't contain the value for 'bill'",
                resultForm.asMap().getFirst("bill"), "burke");
        Assert.assertEquals("The resulting form doesn't contain the value for 'foo'", resultForm.asMap()
                .getFirst("foo"), "bar");
    }

}
