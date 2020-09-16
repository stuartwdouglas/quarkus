package io.quarkus.rest.test.providers.jackson2;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.spi.util.Types;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.jackson2.resource.ExceptionMapperIOExceptionMapper;
import io.quarkus.rest.test.providers.jackson2.resource.ExceptionMapperMarshalErrorMessage;
import io.quarkus.rest.test.providers.jackson2.resource.ExceptionMapperMarshalMyCustomException;
import io.quarkus.rest.test.providers.jackson2.resource.ExceptionMapperMarshalMyCustomExceptionMapper;
import io.quarkus.rest.test.providers.jackson2.resource.ExceptionMapperMarshalName;
import io.quarkus.rest.test.providers.jackson2.resource.ExceptionMapperMarshalResource;
import io.quarkus.rest.test.providers.jackson2.resource.MyEntity;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jackson2 provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-937
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Exception Mapper Marshal Test")
public class ExceptionMapperMarshalTest {

    protected static final Logger logger = Logger.getLogger(ProxyWithGenericReturnTypeJacksonTest.class.getName());

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(Jackson2Test.class);
            Map<String, String> contextParam = new HashMap<>();
            contextParam.put(ResteasyContextParameters.RESTEASY_PREFER_JACKSON_OVER_JSONB, "true");
            return TestUtil.finishContainerPrepare(war, contextParam, ExceptionMapperMarshalErrorMessage.class,
                    ExceptionMapperMarshalMyCustomException.class, MyEntity.class, ExceptionMapperIOExceptionMapper.class,
                    ExceptionMapperMarshalMyCustomExceptionMapper.class, ExceptionMapperMarshalName.class,
                    ExceptionMapperMarshalResource.class);
        }
    });

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ProxyWithGenericReturnTypeJacksonTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Tests usage of custom ExceptionMapper producing json response
     * @tpPassCrit The resource returns Success response
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Custom Used")
    public void testCustomUsed() {
        Type exceptionType = Types.getActualTypeArgumentsOfAnInterface(ExceptionMapperMarshalMyCustomExceptionMapper.class,
                ExceptionMapper.class)[0];
        Assertions.assertEquals(ExceptionMapperMarshalMyCustomException.class, exceptionType);
        Response response = client.target(generateURL("/resource/custom")).request().get();
        Assertions.assertEquals(response.getStatus(), Status.OK.getStatusCode());
        List<ExceptionMapperMarshalErrorMessage> errors = response
                .readEntity(new GenericType<List<ExceptionMapperMarshalErrorMessage>>() {
                });
        Assertions.assertEquals("error", errors.get(0).getError(), "The response has unexpected content");
    }

    @Test
    @DisplayName("Test My Custom Used")
    public void testMyCustomUsed() {
        Response response = client.target(generateURL("/resource/customME")).request().get();
        String text = response.readEntity(String.class);
        Assertions.assertEquals(response.getStatus(), Status.OK.getStatusCode());
        Assertions.assertTrue(text.contains("UN_KNOWN_ERR"), "Response does not contain UN_KNOWN_ERR");
    }
}
