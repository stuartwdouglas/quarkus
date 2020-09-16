package io.quarkus.rest.test.resource.param;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.Assert;
import io.quarkus.rest.test.resource.param.resource.HeaderParamMyClass;
import io.quarkus.rest.test.resource.param.resource.HeaderParamParamConverterProvider;
import io.quarkus.rest.test.resource.param.resource.HeaderParamParamConverterTestService;
import io.quarkus.rest.test.resource.param.resource.HeaderParamParamConverterTestServiceImpl;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Provides a ParamConverter for an input parameter using annotation @HeaderParam
 */
public class HeaderParamParamConverterTest {
    private static String testSimpleName = HeaderParamParamConverterTest.class.getSimpleName();
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(HeaderParamMyClass.class,
                            HeaderParamParamConverterProvider.class,
                            HeaderParamParamConverterTestServiceImpl.class,
                            HeaderParamParamConverterTestService.class);
                    return TestUtil.finishContainerPrepare(war, null, null);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, testSimpleName);
    }

    private static String generateBaseUrl() {
        return PortProviderUtil.generateBaseUrl(testSimpleName);
    }

    @Test
    public void testOne() throws Exception {
        HeaderParamMyClass header = new HeaderParamMyClass();
        header.setValue("someValue");
        // test
        QuarkusRestClient proxyClient = (QuarkusRestClient) ClientBuilder.newClient();
        HeaderParamParamConverterTestService service = proxyClient.target(generateBaseUrl())
                .proxyBuilder(HeaderParamParamConverterTestService.class).build();

        Assert.assertTrue(service.test(header));
        proxyClient.close();
    }
}
