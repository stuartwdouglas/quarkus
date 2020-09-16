package io.quarkus.rest.test.client.proxy;

import java.math.BigDecimal;
import java.util.Date;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.client.jaxrs.ProxyBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.client.proxy.resource.ProxyJaxbResourceMessageResource;
import io.quarkus.rest.test.client.proxy.resource.ProxyJaxbResourcePostMessage;
import io.quarkus.rest.test.client.proxy.resource.ProxyJaxbResourcePostMessageIntf;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class ProxyJaxbResourcePostTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(ProxyJaxbResourcePostMessageIntf.class);
                    return TestUtil.finishContainerPrepare(war, null, ProxyJaxbResourcePostMessage.class,
                            ProxyJaxbResourceMessageResource.class);
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
        return PortProviderUtil.generateURL(path, ProxyJaxbResourcePostTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Tests client proxy with annotated jaxb resource, sends jaxb object to the server
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testClient() throws Exception {
        ProxyJaxbResourcePostMessage m = new ProxyJaxbResourcePostMessage();
        m.setMsgId(new BigDecimal(42));
        m.setCreatedDate(new Date());
        m.setDestinationId("ABCD1234");
        m.setMsgComp(new BigDecimal(2));
        m.setNumLocTfmsProvided(new BigDecimal(14));
        m.setSourceId("WXYZ6789");
        m.setVersionMajor("4");
        m.setVersionMinor("1");
        ProxyJaxbResourcePostMessageIntf proxy = ProxyBuilder
                .builder(ProxyJaxbResourcePostMessageIntf.class, client.target(generateURL("/"))).build();
        Response r = proxy.saveMessage(m);
        Assert.assertEquals(r.getStatus(), Status.CREATED.getStatusCode());
    }
}
