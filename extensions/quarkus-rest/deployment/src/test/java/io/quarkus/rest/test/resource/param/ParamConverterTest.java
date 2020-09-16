package io.quarkus.rest.test.resource.param;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.resource.param.resource.ParamConverterClient;
import io.quarkus.rest.test.resource.param.resource.ParamConverterDefaultClient;
import io.quarkus.rest.test.resource.param.resource.ParamConverterDefaultResource;
import io.quarkus.rest.test.resource.param.resource.ParamConverterPOJO;
import io.quarkus.rest.test.resource.param.resource.ParamConverterPOJOConverter;
import io.quarkus.rest.test.resource.param.resource.ParamConverterPOJOConverterProvider;
import io.quarkus.rest.test.resource.param.resource.ParamConverterResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Parameters
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for ParamConverter
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Param Converter Test")
public class ParamConverterTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(ParamConverterPOJOConverter.class);
            war.addClass(ParamConverterPOJO.class);
            war.addClass(ParamConverterDefaultClient.class);
            war.addClass(ParamConverterClient.class);
            return TestUtil.finishContainerPrepare(war, null, ParamConverterPOJOConverterProvider.class,
                    ParamConverterResource.class, ParamConverterDefaultResource.class);
        }
    });

    private String generateBaseUrl() {
        return PortProviderUtil.generateBaseUrl(ParamConverterTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Set specific values
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test It")
    public void testIt() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        ParamConverterClient proxy = client.target(generateBaseUrl()).proxy(ParamConverterClient.class);
        proxy.put("pojo", "pojo", "pojo", "pojo");
        client.close();
    }

    /**
     * @tpTestDetails Check default values
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Default")
    public void testDefault() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        ParamConverterDefaultClient proxy = client.target(generateBaseUrl()).proxy(ParamConverterDefaultClient.class);
        proxy.put();
        client.close();
    }
}
