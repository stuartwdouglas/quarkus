package io.quarkus.rest.test.providers.multipart;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.multipart.resource.ProxyApiService;
import io.quarkus.rest.test.providers.multipart.resource.ProxyAttachment;
import io.quarkus.rest.test.providers.multipart.resource.ProxyResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Multipart provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test proxy with multipart provider
 * @tpSince RESTEasy 3.0.16
 */
public class ProxyTest {

    private static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(ProxyApiService.class);
                    war.addClass(ProxyAttachment.class);
                    return TestUtil.finishContainerPrepare(war, null, ProxyResource.class);
                }
            });

    private static String generateBaseUrl() {
        return PortProviderUtil.generateBaseUrl(ProxyTest.class.getSimpleName());
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
     * @tpTestDetails ProxyAttachment object and string object is in request
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testNewBuilder() {
        ProxyApiService apiService = client.target(generateBaseUrl()).proxy(ProxyApiService.class);
        tryCall(apiService);
    }

    private void tryCall(ProxyApiService apiService) {
        ProxyAttachment attachment = new ProxyAttachment();
        attachment.setData("foo".getBytes());
        apiService.postAttachment(attachment, "some-key"); // any exception in ProxyResource would be thrown from proxy too, no assert needed
    }

}
