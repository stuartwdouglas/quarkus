package io.quarkus.rest.test.exception;

import java.lang.reflect.Type;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import org.jboss.resteasy.spi.util.Types;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.exception.resource.AbstractMapper;
import io.quarkus.rest.test.exception.resource.AbstractMapperDefault;
import io.quarkus.rest.test.exception.resource.AbstractMapperException;
import io.quarkus.rest.test.exception.resource.AbstractMapperMyCustom;
import io.quarkus.rest.test.exception.resource.AbstractMapperResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 * @tpTestCaseDetails Regression test for RESTEASY-666
 */
@DisplayName("Abstract Exception Mapper Test")
public class AbstractExceptionMapperTest {

    private Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(PortProviderUtil.class);
            war.addClasses(AbstractMapper.class, AbstractMapperException.class);
            return TestUtil.finishContainerPrepare(war, null, AbstractMapperDefault.class, AbstractMapperMyCustom.class,
                    AbstractMapperResource.class);
        }
    });

    @BeforeEach
    public void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterEach
    public void cleanup() {
        client.close();
    }

    /**
     * @tpTestDetails Correct exception mapper should be chosen when ExceptionMapper implement statement is in abstract class.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Custom Used")
    public void testCustomUsed() {
        Type exceptionType = Types.getActualTypeArgumentsOfAnInterface(AbstractMapperMyCustom.class, ExceptionMapper.class)[0];
        Assertions.assertEquals(AbstractMapperException.class, exceptionType);
        Response response = client
                .target(PortProviderUtil.generateURL("/resource/custom", AbstractExceptionMapperTest.class.getSimpleName()))
                .request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "custom");
    }
}
