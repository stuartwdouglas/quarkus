package io.quarkus.rest.test.client.proxy;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;

import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.client.proxy.resource.ClientSmokeResource;
import io.quarkus.rest.test.core.smoke.resource.ResourceWithInterfaceSimpleClient;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Smoke tests for jaxrs
 * @tpChapter Integration tests
 * @tpTestCaseDetails Smoke test for client ProxyFactory.
 * @tpSince RESTEasy 3.0.16
 */
public class ClientSmokeTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, ClientSmokeResource.class);
                }
            });

    /**
     * @tpTestDetails Check results from ResourceWithInterfaceSimpleClient.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testNoDefaultsResource() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        ResourceWithInterfaceSimpleClient proxy = client.target(
                PortProviderUtil.generateBaseUrl(ClientSmokeTest.class.getSimpleName()))
                .proxyBuilder(ResourceWithInterfaceSimpleClient.class).build();

        Assert.assertEquals("Wrong client answer.", "basic", proxy.getBasic());
        proxy.putBasic("hello world");
        Assert.assertEquals("Wrong client answer.", "hello world", proxy.getQueryParam("hello world"));
        Assert.assertEquals("Wrong client answer.", 1234, proxy.getUriParam(1234));

        client.close();
    }

}
