package io.quarkus.rest.test.client.proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.client.proxy.resource.ProxyWithGenericReturnTypeInvocationHandler;
import io.quarkus.rest.test.client.proxy.resource.ProxyWithGenericReturnTypeMessageBodyWriter;
import io.quarkus.rest.test.client.proxy.resource.ProxyWithGenericReturnTypeResource;
import io.quarkus.rest.test.client.proxy.resource.ProxyWithGenericReturnTypeSubResourceIntf;
import io.quarkus.rest.test.client.proxy.resource.ProxyWithGenericReturnTypeSubResourceSubIntf;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpTestCaseDetails Test for generic proxy return type. Proxy is set on server (not on client).
 * @tpSince RESTEasy 3.0.16
 */
public class ProxyWithGenericReturnTypeTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(ProxyWithGenericReturnTypeInvocationHandler.class);
                    war.addClass(ProxyWithGenericReturnTypeSubResourceIntf.class);
                    war.addClass(ProxyWithGenericReturnTypeSubResourceSubIntf.class);

                    List<Class<?>> singletons = new ArrayList<>();
                    singletons.add(ProxyWithGenericReturnTypeMessageBodyWriter.class);
                    return TestUtil.finishContainerPrepare(war, null, singletons, ProxyWithGenericReturnTypeResource.class);
                }
            });

    /**
     * @tpTestDetails Test for new client
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void newClientTest() throws Exception {
        Client client = ClientBuilder.newClient();

        WebTarget base = client
                .target(PortProviderUtil.generateURL("/test/list/", ProxyWithGenericReturnTypeTest.class.getSimpleName()));
        Response response = base.request().get();

        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertTrue("Wrong content of response, list was not decoden on server",
                response.readEntity(String.class).indexOf("List<String>") >= 0);

        client.close();
    }
}
