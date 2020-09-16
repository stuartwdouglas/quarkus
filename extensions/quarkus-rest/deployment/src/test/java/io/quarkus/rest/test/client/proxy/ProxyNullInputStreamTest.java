package io.quarkus.rest.test.client.proxy;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.Assert;
import io.quarkus.rest.test.client.proxy.resource.ProxyNullInputStreamClientResponseFilter;
import io.quarkus.rest.test.client.proxy.resource.ProxyNullInputStreamResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for RESTEASY-1671
 * @tpSince RESTEasy 4.0.0
 *
 *          Created by rsearls on 8/24/17.
 */
public class ProxyNullInputStreamTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(ProxyNullInputStreamResource.class,
                            ProxyNullInputStreamClientResponseFilter.class);
                    return TestUtil.finishContainerPrepare(war, null, (Class<?>[]) null);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ProxyNullInputStreamTest.class.getSimpleName());
    }

    @Test
    public void testNullPointerEx() throws Exception {
        Client client = ClientBuilder.newBuilder().register(ProxyNullInputStreamClientResponseFilter.class).build();
        ProxyNullInputStreamResource pResource = ((QuarkusRestWebTarget) client.target(generateURL("/test/user/mydb")))
                .proxyBuilder(ProxyNullInputStreamResource.class)
                .build();
        try {
            pResource.getUserHead("myDb");
        } catch (Exception e) {
            Assert.assertEquals("HTTP 404 Not Found", e.getMessage());
        } finally {
            client.close();
        }

    }
}
