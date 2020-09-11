package io.quarkus.rest.test.providers.jackson2.jsonfilter;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.JsonFilterChild;
import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.JsonFilterChildResource;
import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.JsonFilterParent;
import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.ObjectFilterModifier;
import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.ObjectWriterModifierFilter;
import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.PersonType;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jackson2 provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Filters fields from json object. Specifies the filter implementation class in web.xml.
 *                    JsonFilterParent defines the @JsonFilter annotation. JsonFilter applies to its subclass JsonFilterChild as
 *                    well.
 * @tpSince RESTEasy 3.1.0
 */
public class JsonFilterSuperClassTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(JsonFilterParent.class, JsonFilterChild.class, PersonType.class, ObjectFilterModifier.class,
                            ObjectWriterModifierFilter.class);
                    war.addAsManifestResource(
                            new StringAsset("Manifest-Version: 1.0\n"
                                    + "Dependencies: com.fasterxml.jackson.jaxrs.jackson-jaxrs-json-provider\n"),
                            "MANIFEST.MF");
                    war.addAsWebInfResource(JsonFilterWithSerlvetFilterTest.class.getPackage(), "web.xml", "web.xml");
                    return TestUtil.finishContainerPrepare(war, null, JsonFilterChildResource.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, JsonFilterSuperClassTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Json string in the response is correctly filtered
     * @tpSince RESTEasy 3.1.0
     */
    @Test
    public void testJacksonStringInSuperClass() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(generateURL("/superclass/333"));
        Response response = target.request().get();
        response.bufferEntity();
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertTrue("Filter doesn't work", !response.readEntity(String.class).contains("id") &&
                response.readEntity(String.class).contains("name"));
        client.close();
    }
}
