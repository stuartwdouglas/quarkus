package io.quarkus.rest.test.providers.custom;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.custom.resource.CustomProviderPreferenceUser;
import io.quarkus.rest.test.providers.custom.resource.CustomProviderPreferenceUserBodyWriter;
import io.quarkus.rest.test.providers.custom.resource.CustomProviderPreferenceUserResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Providers
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for custom provider preference.
 * @tpSince RESTEasy 3.0.16
 */
public class CustomProviderPreferenceTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(CustomProviderPreferenceUser.class);
                    return TestUtil.finishContainerPrepare(war, null, CustomProviderPreferenceUserResource.class,
                            CustomProviderPreferenceUserBodyWriter.class);
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
        return PortProviderUtil.generateURL(path, CustomProviderPreferenceTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Client test.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testCustomProviderPreference() throws Exception {
        Response response = client.target(generateURL("/user")).request().get();
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("Wrong content of response", "jharting;email@example.com", response.readEntity(String.class));
        response.close();
    }
}
