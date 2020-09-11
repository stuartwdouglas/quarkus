package io.quarkus.rest.test.client.proxy;

import java.io.InputStream;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;

import org.jboss.resteasy.util.ReadFromStream;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.client.proxy.resource.ProxyInputStreamProxy;
import io.quarkus.rest.test.client.proxy.resource.ProxyInputStreamResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-351
 * @tpSince RESTEasy 3.0.16
 */
public class ProxyInputStreamTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(ProxyInputStreamProxy.class);
                    return TestUtil.finishContainerPrepare(war, null, ProxyInputStreamResource.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ProxyInputStreamTest.class.getSimpleName());
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
     * @tpTestDetails New client version
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testInputStreamNewClient() throws Exception {
        ProxyInputStreamProxy proxy = client.target(generateURL("/")).proxy(ProxyInputStreamProxy.class);
        InputStream is = proxy.get();
        byte[] bytes = ReadFromStream.readFromStream(100, is);
        is.close();
        String str = new String(bytes);
        Assert.assertEquals("hello world", str);
    }
}
