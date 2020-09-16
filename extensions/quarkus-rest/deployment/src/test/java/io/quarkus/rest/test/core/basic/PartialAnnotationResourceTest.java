package io.quarkus.rest.test.core.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.core.basic.resource.PartialAnnotationResource;
import io.quarkus.rest.test.core.basic.resource.PartialAnnotationResourceImpl;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Configuration
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for RESTEASY-798.
 * @tpSince RESTEasy 3.5.1
 */
public class PartialAnnotationResourceTest {
    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PartialAnnotationResource.class, PartialAnnotationResourceImpl.class);
                    return TestUtil.finishContainerPrepare(war, null, PartialAnnotationResourceImpl.class);
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

    /**
     * @tpTestDetails Test basic functionality of test resource
     * @tpSince RESTEasy 3.5.1
     */
    @Test
    public void test() {
        PartialAnnotationResource proxy = client
                .target(PortProviderUtil.generateBaseUrl(PartialAnnotationResourceTest.class.getSimpleName()))
                .proxy(PartialAnnotationResource.class);
        Assert.assertEquals(PartialAnnotationResourceImpl.BAR_RESPONSE, proxy.bar());
    }
}
