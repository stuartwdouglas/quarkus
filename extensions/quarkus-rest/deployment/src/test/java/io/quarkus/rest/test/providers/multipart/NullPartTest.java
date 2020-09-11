package io.quarkus.rest.test.providers.multipart;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;

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
import io.quarkus.rest.test.providers.multipart.resource.MyServiceProxy;
import io.quarkus.rest.test.providers.multipart.resource.NullPartBean;
import io.quarkus.rest.test.providers.multipart.resource.NullPartService;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Multipart provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test proxy with null part of message with multipart provider
 * @tpSince RESTEasy 3.0.16
 */
public class NullPartTest {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(NullPartBean.class, MyServiceProxy.class);
                    return TestUtil.finishContainerPrepare(war, null, NullPartService.class);
                }
            });

    private static QuarkusRestClient client;

    private static String generateBaseUrl() {
        return PortProviderUtil.generateBaseUrl(NullPartTest.class.getSimpleName());
    }

    @BeforeClass
    public static void before() throws Exception {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterClass
    public static void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Test new client
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testNewClient() throws Exception {
        MyServiceProxy proxy = client.target(generateBaseUrl()).proxy(MyServiceProxy.class);

        NullPartBean bean = proxy.createMyBean(); // should just be ok
        Assert.assertNotNull(bean);
        Assert.assertNull(bean.getSomeBinary());
    }

}
