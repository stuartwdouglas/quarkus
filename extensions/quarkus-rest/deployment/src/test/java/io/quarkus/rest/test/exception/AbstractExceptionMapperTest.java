package io.quarkus.rest.test.exception;

import java.lang.reflect.Type;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.spi.util.Types;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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
public class AbstractExceptionMapperTest {

    private Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(PortProviderUtil.class);
                    war.addClasses(AbstractMapper.class, AbstractMapperException.class);

                    return TestUtil.finishContainerPrepare(war, null, AbstractMapperDefault.class,
                            AbstractMapperMyCustom.class, AbstractMapperResource.class);
                }
            });

    @Before
    public void setup() {
        client = ClientBuilder.newClient();
    }

    @After
    public void cleanup() {
        client.close();
    }

    /**
     * @tpTestDetails Correct exception mapper should be chosen when ExceptionMapper implement statement is in abstract class.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testCustomUsed() {
        Type exceptionType = Types.getActualTypeArgumentsOfAnInterface(AbstractMapperMyCustom.class, ExceptionMapper.class)[0];
        Assert.assertEquals(AbstractMapperException.class, exceptionType);

        Response response = client.target(PortProviderUtil.generateURL("/resource/custom",
                AbstractExceptionMapperTest.class.getSimpleName())).request().get();
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("custom", response.readEntity(String.class));
    }
}
