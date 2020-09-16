package io.quarkus.rest.test.client.other;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;

import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestClientBuilder;
import io.quarkus.rest.test.client.other.resource.ApacheHttpClient4Resource;
import io.quarkus.rest.test.client.other.resource.ApacheHttpClient4ResourceImpl;
import io.quarkus.rest.test.client.other.resource.CustomHttpClientEngineBuilder;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpTestCaseDetails Client engine customization (RESTEASY-1599)
 * @tpSince RESTEasy 3.0.24
 */
public class CustomHttpClientEngineTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(ApacheHttpClient4Resource.class);
                    return TestUtil.finishContainerPrepare(war, null, ApacheHttpClient4ResourceImpl.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, CustomHttpClientEngineTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Create custom ClientHttpEngine and set it to the resteasy-client
     * @tpSince RESTEasy 3.0.24
     */
    @Test
    public void test() {
        QuarkusRestClientBuilder clientBuilder = ((QuarkusRestClientBuilder) ClientBuilder.newBuilder());
        ClientHttpEngine engine = new CustomHttpClientEngineBuilder().QuarkusRestClientBuilder(clientBuilder).build();
        QuarkusRestClient client = clientBuilder.httpEngine(engine).build();
        Assert.assertTrue(ApacheHttpClient43Engine.class.isInstance(client.httpEngine()));

        ApacheHttpClient4Resource proxy = client.target(generateURL("")).proxy(ApacheHttpClient4Resource.class);
        Assert.assertEquals("Unexpected response", "hello world", proxy.get());

        client.close();
    }
}
