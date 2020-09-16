package io.quarkus.rest.test.resource.path;

import java.util.function.Supplier;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.resource.path.resource.LocatorSubResourceReturningThisParamEntityPrototype;
import io.quarkus.rest.test.resource.path.resource.LocatorSubResourceReturningThisParamEntityWithConstructor;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class LocatorSubResourceReturningThisTest {

    @Path("resource")
    public static class LocatorSubResourceReturningThisSubResource extends LocatorSubResourceReturningThisPathParamTest {

        @Path("subresource")
        public LocatorSubResourceReturningThisSubResource subresorce() {
            return this;
        }
    }

    @Path(value = "/PathParamTest")
    public static class LocatorSubResourceReturningThisPathParamTest {

        @Produces(MediaType.TEXT_PLAIN)
        @GET
        @Path("/ParamEntityWithConstructor/{id}")
        public String paramEntityWithConstructorTest(
                @DefaultValue("PathParamTest") @PathParam("id") LocatorSubResourceReturningThisParamEntityWithConstructor paramEntityWithConstructor) {
            return paramEntityWithConstructor.getValue();
        }
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(LocatorSubResourceReturningThisPathParamTest.class,
                            LocatorSubResourceReturningThisParamEntityPrototype.class,
                            LocatorSubResourceReturningThisParamEntityWithConstructor.class);
                    return TestUtil.finishContainerPrepare(war, null, LocatorSubResourceReturningThisSubResource.class);
                }
            });

    static Client client;

    @BeforeClass
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterClass
    public static void close() {
        client.close();
    }

    /**
     * @tpTestDetails Client sends GET request for the resource Locator, which returns itself. The Resource Locator here
     *                extends the resource with HTTP methods annotations directly.
     * @tpPassCrit Correct response is returned from the server
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void LocatorWithSubWithPathAnnotationTest() {
        Response response = client.target(PortProviderUtil.generateURL(
                "/resource/subresource/ParamEntityWithConstructor/ParamEntityWithConstructor=JAXRS",
                LocatorSubResourceReturningThisTest.class.getSimpleName())).request().get();
        Assert.assertEquals(200, response.getStatus());
        response.close();
    }

}
