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
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.exception.resource.ExceptionMapperAbstractExceptionMapper;
import io.quarkus.rest.test.exception.resource.ExceptionMapperMyCustomException;
import io.quarkus.rest.test.exception.resource.ExceptionMapperMyCustomExceptionMapper;
import io.quarkus.rest.test.exception.resource.ExceptionMapperMyCustomSubException;
import io.quarkus.rest.test.exception.resource.ExceptionMapperResource;
import io.quarkus.rest.test.exception.resource.ExceptionMapperWebAppExceptionMapper;
import io.quarkus.rest.test.exception.resource.NotFoundExceptionMapper;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class ExceptionMapperTest {

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(ExceptionMapperAbstractExceptionMapper.class);
                    return TestUtil.finishContainerPrepare(war, null, ExceptionMapperResource.class,
                            ExceptionMapperWebAppExceptionMapper.class,
                            ExceptionMapperMyCustomExceptionMapper.class, ExceptionMapperMyCustomException.class,
                            ExceptionMapperMyCustomSubException.class, NotFoundExceptionMapper.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ExceptionMapperTest.class.getSimpleName());
    }

    @BeforeClass
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterClass
    public static void close() {
        client.close();
    }

    /**
     * @tpTestDetails Client sends GET request to the server, which causes application custom exception being thrown, this
     *                exception is caught by application provided ExceptionMapper. The application provides two providers for
     *                mapping
     *                exceptions. General RuntimeException mapping and MyCustomException mapping which extends the
     *                RuntimeException.
     * @tpPassCrit The more specific MyCustomException mapping will be used.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testCustomExceptionsUsed() {
        Type exceptionType = Types.getActualTypeArgumentsOfAnInterface(ExceptionMapperMyCustomExceptionMapper.class,
                ExceptionMapper.class)[0];
        Assert.assertEquals(ExceptionMapperMyCustomException.class, exceptionType);
        Response response = client.target(generateURL("/resource/custom")).request().get();
        Assert.assertNotEquals("General RuntimeException mapper was used instead of more specific one",
                Status.NOT_ACCEPTABLE, response.getStatus());
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals("custom", response.readEntity(String.class));
    }

    /**
     * @tpTestDetails Client sends GET request to the server, which causes WebapplicationException being thrown, this
     *                exception is caught by application provided ExceptionMapper
     * @tpPassCrit Application provided ExceptionMapper serves the exception and creates response with ACCEPTED status
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testWAEResponseUsed() {
        Response response = client.target(generateURL("/resource/responseok")).request().get();
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals("hello", response.readEntity(String.class));
    }

    /**
     * @tpTestDetails Client sends GET request to the server, which causes the subclass of an exception
     *                which has an ExceptionMapper to be thrown. This subclass exception is caught by application provided
     *                ExceptionMapper
     * @tpPassCrit Application provided ExceptionMapper serves the exception and creates response with ACCEPTED status
     * @tpSince RESTEasy 3.0.20
     */
    @Test
    public void testCustomSubExceptionsUsed() {
        Response response = client.target(generateURL("/resource/sub")).request().get();
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals("custom", response.readEntity(String.class));
    }

    /**
     * @tpTestDetails Client sends GET request to a nonexistent resource, which causes a NotFoundException to be thrown.
     *                The NotFoundException is caught by the application provided NotFoundExceptionMapper, which sends a 410
     *                status.
     * @tpSince RESTEasy 3.0.20
     */
    @Test
    public void testNotFoundExceptionMapping() {
        Response response = client.target(generateURL("/bogus")).request().get();
        Assert.assertEquals(410, response.getStatus());
        response.close();
    }
}
