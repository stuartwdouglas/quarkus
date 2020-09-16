package io.quarkus.rest.test.providers.multipart;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.providers.multipart.resource.InputPartDefaultContentTypeWildcardOverwriteContainerBean;
import io.quarkus.rest.test.providers.multipart.resource.InputPartDefaultContentTypeWildcardOverwriteNewInterceptor;
import io.quarkus.rest.test.providers.multipart.resource.InputPartDefaultContentTypeWildcardOverwriteService;
import io.quarkus.rest.test.providers.multipart.resource.InputPartDefaultContentTypeWildcardOverwriteXmlBean;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Multipart provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails MultiPart provider should be able to process xml, if wildcard is set. Wildcard is set in new version of
 *                    interceptor.
 * @tpSince RESTEasy 3.0.16
 */
public class InputPartDefaultContentTypeWildcardOverwriteNewInterceptorTest {

    public static final String WILDCARD_WITH_CHARSET_UTF_8 = MediaType.APPLICATION_XML + "; charset=UTF-8"; // this mediatype works correctly
    private static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(InputPartDefaultContentTypeWildcardOverwriteContainerBean.class);
                    war.addClasses(InputPartDefaultContentTypeWildcardOverwriteXmlBean.class,
                            InputPartDefaultContentTypeWildcardOverwriteNewInterceptorTest.class);
                    return TestUtil.finishContainerPrepare(war, null,
                            InputPartDefaultContentTypeWildcardOverwriteNewInterceptor.class,
                            InputPartDefaultContentTypeWildcardOverwriteService.class);
                }
            });

    @BeforeClass
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
    }

    @AfterClass
    public static void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Test for new client
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testContentTypeNewClient() throws Exception {
        String message = "--boo\r\n"
                + "Content-Disposition: form-data; name=\"foo\"\r\n"
                + "Content-Transfer-Encoding: 8bit\r\n\r\n"
                + "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<inputPartDefaultContentTypeWildcardOverwriteXmlBean><myInt>27</myInt><myString>Lorem Ipsum</myString></inputPartDefaultContentTypeWildcardOverwriteXmlBean>\r\n"
                + "--boo--\r\n";

        WebTarget target = client.target(PortProviderUtil.generateURL("/mime",
                InputPartDefaultContentTypeWildcardOverwriteNewInterceptorTest.class.getSimpleName()));
        Entity entity = Entity.entity(message, "multipart/form-data; boundary=boo");
        Response response = target.request().post(entity);

        Assert.assertEquals("MultiPart provider is unable to process xml, if media type is set in interceptor",
                20, response.getStatus() / 10);
        Assert.assertEquals("Response text is wrong", "27", response.readEntity(String.class));
    }
}
