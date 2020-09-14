package io.quarkus.rest.test.microprofile.restclient;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.microprofile.restclient.resource.RestClientProxyRedeployRemoteService;
import io.quarkus.rest.test.microprofile.restclient.resource.RestClientProxyRedeployResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

public class RestClientProxyRedeployTest {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(RestClientProxyRedeployRemoteService.class);
                    war.addAsManifestResource(new StringAsset("Dependencies: org.eclipse.microprofile.restclient"),
                            "MANIFEST.MF");

                    return TestUtil.finishContainerPrepare(war, null, RestClientProxyRedeployResource.class);
                }
            });

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(RestClientProxyRedeployRemoteService.class);
                    war.addAsManifestResource(new StringAsset("Dependencies: org.eclipse.microprofile.restclient"),
                            "MANIFEST.MF");

                    return TestUtil.finishContainerPrepare(war, null, RestClientProxyRedeployResource.class);
                }
            });

    private String generateURL(String path, String suffix) {
        return PortProviderUtil.generateURL(path, RestClientProxyRedeployTest.class.getSimpleName() + suffix);
    }

    @Test
    public void testGet1() throws Exception {
        Client client = ClientBuilder.newClient();
        Response response = client.target(generateURL("/test/1", "1")).request().get();
        Assert.assertEquals(200, response.getStatus());
        String entity = response.readEntity(String.class);
        Assert.assertEquals("OK", entity);
        client.close();
    }

    @Test
    public void testGet2() throws Exception {
        Client client = ClientBuilder.newClient();
        Response response = client.target(generateURL("/test/1", "2")).request().get();
        Assert.assertEquals(200, response.getStatus());
        String entity = response.readEntity(String.class);
        Assert.assertEquals("OK", entity);
        client.close();
    }
}
