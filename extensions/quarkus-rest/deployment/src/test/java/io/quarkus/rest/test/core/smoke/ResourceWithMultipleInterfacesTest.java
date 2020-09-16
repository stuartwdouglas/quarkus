package io.quarkus.rest.test.core.smoke;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.core.smoke.resource.ResourceWithMultipleInterfacesEmpty;
import io.quarkus.rest.test.core.smoke.resource.ResourceWithMultipleInterfacesIntA;
import io.quarkus.rest.test.core.smoke.resource.ResourceWithMultipleInterfacesRootResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Smoke tests for jaxrs
 * @tpChapter Integration tests
 * @tpTestCaseDetails Smoke test for resource with multiple interfaces.
 * @tpSince RESTEasy 3.0.16
 */
public class ResourceWithMultipleInterfacesTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(ResourceWithMultipleInterfacesIntA.class);
                    war.addClass(ResourceWithMultipleInterfacesEmpty.class);
                    return TestUtil.finishContainerPrepare(war, null, ResourceWithMultipleInterfacesRootResource.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ResourceWithMultipleInterfacesTest.class.getSimpleName());
    }

    @Before
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Check result from resource with multiple interfaces.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testNoDefaultsResource() throws Exception {
        ResourceWithMultipleInterfacesIntA proxy = client.target(generateURL("/"))
                .proxy(ResourceWithMultipleInterfacesIntA.class);
        Assert.assertEquals("Wrong client answer.", "FOO", proxy.getFoo());
    }
}
