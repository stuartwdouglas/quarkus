package io.quarkus.rest.test.cdi.injection;

import java.net.URI;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.cdi.injection.resource.LazyInitUriInfoInjectionResource;
import io.quarkus.rest.test.cdi.injection.resource.LazyInitUriInfoInjectionSingletonResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Injection
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-573
 * @tpSince RESTEasy 3.0.16
 */
public class LazyInitUriInfoInjectionTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, LazyInitUriInfoInjectionSingletonResource.class,
                            LazyInitUriInfoInjectionResource.class);
                }
            });
    @ArquillianResource
    URI baseUri;

    private String generateURL(String path) {
        return baseUri.resolve(path).toString();
    }

    /**
     * @tpTestDetails Repeat client request without query parameter
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testDup() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        WebTarget base = client.target(generateURL("test?h=world"));
        String val = base.request().get().readEntity(String.class);
        Assert.assertEquals(val, "world");

        base = client.target(generateURL("test"));
        val = base.request().get().readEntity(String.class);
        Assert.assertEquals(val, "");
        client.close();
    }
}
