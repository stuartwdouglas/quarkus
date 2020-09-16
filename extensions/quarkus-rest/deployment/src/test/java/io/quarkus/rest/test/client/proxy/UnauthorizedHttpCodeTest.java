package io.quarkus.rest.test.client.proxy;

import java.util.function.Supplier;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.Assert;
import io.quarkus.rest.test.client.proxy.resource.UnauthorizedHttpCodeProxy;
import io.quarkus.rest.test.client.proxy.resource.UnauthorizedHttpCodeResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpSince RESTEasy 3.0.16
 * @tpTestCaseDetails Regression test for RESTEASY-575
 */
public class UnauthorizedHttpCodeTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, UnauthorizedHttpCodeResource.class);
                }
            });

    /**
     * @tpTestDetails Get 401 http code via proxy
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testProxy() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        UnauthorizedHttpCodeProxy proxy = client
                .target(PortProviderUtil.generateURL("/", UnauthorizedHttpCodeTest.class.getSimpleName()))
                .proxy(UnauthorizedHttpCodeProxy.class);
        try {
            proxy.getFoo();
        } catch (NotAuthorizedException e) {
            Assert.assertEquals(e.getResponse().getStatus(), Status.UNAUTHORIZED.getStatusCode());
            String val = e.getResponse().readEntity(String.class);
            Assert.assertEquals("Wrong content of response", "hello", val);
        }
        client.close();
    }

}
