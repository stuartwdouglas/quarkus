package io.quarkus.rest.test.client.proxy;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.client.jaxrs.ProxyBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.client.proxy.resource.ProxyJaxbCredit;
import io.quarkus.rest.test.client.proxy.resource.ProxyJaxbResource;
import io.quarkus.rest.test.client.proxy.resource.ProxyJaxbResourceIntf;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class ProxyJaxbResourceIntfTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(ProxyJaxbResourceIntfTest.class);
                    war.addClass(ProxyJaxbResourceIntf.class);
                    return TestUtil.finishContainerPrepare(war, null, ProxyJaxbResource.class,
                            ProxyJaxbCredit.class);
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

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ProxyJaxbResourceIntfTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Tests client proxy with annotated jaxb resource, RESTEASY-306
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testIt() throws Exception {
        ProxyJaxbResourceIntf proxy = ProxyBuilder.builder(ProxyJaxbResourceIntf.class, client.target(generateURL("/")))
                .build();
        Response response = proxy.getCredits("xx");
        Assert.assertEquals(response.getStatus(), Status.OK.getStatusCode());
        ProxyJaxbCredit cred = response.readEntity(ProxyJaxbCredit.class);
        Assert.assertEquals("Unexpected response from the server", "foobar", cred.getName());
    }

}
