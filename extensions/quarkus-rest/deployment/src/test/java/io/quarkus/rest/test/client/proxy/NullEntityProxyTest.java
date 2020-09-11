package io.quarkus.rest.test.client.proxy;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;

import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.client.proxy.resource.NullEntityProxy;
import io.quarkus.rest.test.client.proxy.resource.NullEntityProxyGreeter;
import io.quarkus.rest.test.client.proxy.resource.NullEntityProxyGreeting;
import io.quarkus.rest.test.client.proxy.resource.NullEntityProxyResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1684
 * @tpSince RESTEasy 3.0.24
 */
public class NullEntityProxyTest {

    private static QuarkusRestClient client;

    @BeforeClass
    public static void before() throws Exception {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterClass
    public static void after() throws Exception {
        client.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(NullEntityProxy.class, NullEntityProxyGreeting.class, NullEntityProxyGreeter.class);
                    return TestUtil.finishContainerPrepare(war, null, NullEntityProxyResource.class);
                }
            });

    private static String generateBaseUrl() {
        return PortProviderUtil.generateBaseUrl(NullEntityProxyTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test to send null Entity with proxy
     * @tpSince RESTEasy 3.0.24
     */
    @Test
    public void testNullEntityWithProxy() {
        ResteasyWebTarget target = client.target(generateBaseUrl());
        NullEntityProxy proxy = target.proxy(NullEntityProxy.class);
        NullEntityProxyGreeting greeting = proxy.helloEntity(null);
        Assert.assertEquals("Response has wrong content", null, greeting.getGreeter());
    }
}
