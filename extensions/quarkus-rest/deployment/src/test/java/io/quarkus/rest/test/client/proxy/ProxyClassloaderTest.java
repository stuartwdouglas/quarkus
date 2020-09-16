package io.quarkus.rest.test.client.proxy;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.client.proxy.resource.ClassloaderResource;
import io.quarkus.rest.test.client.proxy.resource.ClientSmokeResource;
import io.quarkus.rest.test.core.smoke.resource.ResourceWithInterfaceSimpleClient;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;

public class ProxyClassloaderTest {

    @Deployment
    public static Archive<?> deploySimpleResource() {
        WebArchive war = TestUtil.prepareArchive(ProxyClassloaderTest.class.getSimpleName());
        war.addClass(ResourceWithInterfaceSimpleClient.class);

        return TestUtil.finishContainerPrepare(war, null, ClientSmokeResource.class, ClassloaderResource.class);
    }

    @Test
    public void testNoTCCL() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        String target2 = PortProviderUtil.generateURL("", ProxyClassloaderTest.class.getSimpleName());
        String target = PortProviderUtil.generateURL("/cl/cl?param=" + target2, ProxyClassloaderTest.class.getSimpleName());
        Response response = client.target(target).request().get();
        String entity = response.readEntity(String.class);
        Assert.assertEquals("basic", entity);
        response.close();
        client.close();
    }

}
