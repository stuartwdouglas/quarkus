package io.quarkus.rest.test.client.proxy;

import static org.junit.Assert.assertEquals;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.jboss.resteasy.client.jaxrs.ProxyBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.client.proxy.resource.ProxyCastingInterfaceA;
import io.quarkus.rest.test.client.proxy.resource.ProxyCastingInterfaceB;
import io.quarkus.rest.test.client.proxy.resource.ProxyCastingNothing;
import io.quarkus.rest.test.client.proxy.resource.ProxyCastingResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Configuration
 * @tpChapter Integration tests
 * @tpTestCaseDetails Any interface could be cast to QuarkusRestClientProxy.
 *                    JBEAP-3197, JBEAP-4700
 * @tpSince RESTEasy 3.0.16
 */
public class ProxyCastingTest {
    private static Client client;
    private static QuarkusRestWebTarget target;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(ProxyCastingNothing.class,
                            ProxyCastingInterfaceA.class, ProxyCastingInterfaceB.class);
                    return TestUtil.finishContainerPrepare(war, null, ProxyCastingResource.class);
                }
            });

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ProxyCastingTest.class.getSimpleName());
    }

    @BeforeClass
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
        target = (QuarkusRestWebTarget) client.target(generateURL("/foobar"));
    }

    @AfterClass
    public static void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Cast one proxy to other proxy. New client.
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    public void testNewClient() throws Exception {
        ProxyCastingInterfaceA a = ProxyBuilder.builder(ProxyCastingInterfaceA.class, target).build();
        assertEquals("FOO", a.getFoo());
        ProxyCastingInterfaceB b = ((org.jboss.resteasy.client.jaxrs.internal.proxy.QuarkusRestClientProxy) a)
                .as(ProxyCastingInterfaceB.class);
        assertEquals("BAR", b.getBar());
    }
}
