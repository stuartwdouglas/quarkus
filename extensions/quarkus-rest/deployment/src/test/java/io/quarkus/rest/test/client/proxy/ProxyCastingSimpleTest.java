package io.quarkus.rest.test.client.proxy;

import static org.junit.Assert.assertEquals;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.jboss.resteasy.client.jaxrs.ProxyBuilder;
import org.jboss.resteasy.client.jaxrs.internal.proxy.QuarkusRestClientProxy;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.client.proxy.resource.ProxyCastingInterfaceB;
import io.quarkus.rest.test.client.proxy.resource.ProxyCastingSimpleFooBar;
import io.quarkus.rest.test.client.proxy.resource.ProxyCastingSimpleFooBarImpl;
import io.quarkus.rest.test.client.proxy.resource.ProxyCastingSimpleInterfaceA;
import io.quarkus.rest.test.client.proxy.resource.ProxyCastingSimpleInterfaceAorB;
import io.quarkus.rest.test.client.proxy.resource.ProxyCastingSimpleInterfaceB;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Configuration
 * @tpChapter Integration tests
 * @tpTestCaseDetails Any interface could be cast to QuarkusRestClientProxy.
 *                    JBEAP-3197, JBEAP-4700
 * @tpSince RESTEasy 3.0.17
 */
public class ProxyCastingSimpleTest {
    private static Client client;
    private static QuarkusRestWebTarget target;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(ProxyCastingSimpleFooBar.class,
                            ProxyCastingSimpleFooBar.class, ProxyCastingInterfaceB.class,
                            ProxyCastingSimpleInterfaceA.class, ProxyCastingSimpleInterfaceAorB.class,
                            ProxyCastingSimpleInterfaceB.class);
                    return TestUtil.finishContainerPrepare(war, null, ProxyCastingSimpleFooBarImpl.class);
                }
            });

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ProxyCastingSimpleTest.class.getSimpleName());
    }

    @BeforeAll
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
        target = (QuarkusRestWebTarget) client.target(generateURL("/foobar"));
    }

    @AfterAll
    public static void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Cast one proxy to other proxy. Old client.
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    public void testSubresourceProxy() throws Exception {
        ProxyCastingSimpleFooBar foobar = ProxyBuilder.builder(ProxyCastingSimpleFooBar.class, target).build();
        {
            ProxyCastingSimpleInterfaceA a = ((QuarkusRestClientProxy) foobar.getThing("a"))
                    .as(ProxyCastingSimpleInterfaceA.class);
            assertEquals("Wrong body of response", "FOO", a.getFoo());
            ProxyCastingSimpleInterfaceB b = ((QuarkusRestClientProxy) foobar.getThing("b"))
                    .as(ProxyCastingSimpleInterfaceB.class);
            assertEquals("Wrong body of response", "BAR", b.getBar());
        }
        {
            ProxyCastingSimpleInterfaceA a = foobar.getThing("a").as(ProxyCastingSimpleInterfaceA.class);
            assertEquals("Wrong body of response", "FOO", a.getFoo());
            ProxyCastingSimpleInterfaceB b = foobar.getThing("b").as(ProxyCastingSimpleInterfaceB.class);
            assertEquals("Wrong body of response", "BAR", b.getBar());
        }
    }
}
