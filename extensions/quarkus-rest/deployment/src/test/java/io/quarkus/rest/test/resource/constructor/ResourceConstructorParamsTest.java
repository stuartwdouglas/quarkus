package io.quarkus.rest.test.resource.constructor;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.resource.constructor.resource.ConstructorCookieParamWAEResource;
import io.quarkus.rest.test.resource.constructor.resource.ConstructorNoParamsResource;
import io.quarkus.rest.test.resource.constructor.resource.ConstructorParams400Resource;
import io.quarkus.rest.test.resource.constructor.resource.ConstructorParams404Resource;
import io.quarkus.rest.test.resource.constructor.resource.ConstructorParamsMixedResource;
import io.quarkus.rest.test.resource.constructor.resource.ConstructorQueryParamWAEResource;
import io.quarkus.rest.test.resource.constructor.resource.Item;
import io.quarkus.rest.test.resource.constructor.resource.Item2;
import io.quarkus.rest.test.resource.constructor.resource.Item2ParamConverterProvider;
import io.quarkus.rest.test.resource.constructor.resource.ItemParamConverterProvider;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

@DisplayName("Resource Constructor Params Test")
public class ResourceConstructorParamsTest {

    protected static final Logger logger = Logger.getLogger(ResourceConstructorParamsTest.class.getName());

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(ConstructorParamsMixedResource.class);
            war.addClass(ConstructorNoParamsResource.class);
            war.addClass(ItemParamConverterProvider.class);
            war.addClass(Item.class);
            war.addClass(Item2ParamConverterProvider.class);
            war.addClass(Item2.class);
            war.addClass(ConstructorParams404Resource.class);
            war.addClass(ConstructorParams400Resource.class);
            war.addClass(ConstructorCookieParamWAEResource.class);
            war.addClass(ConstructorQueryParamWAEResource.class);
            return TestUtil.finishContainerPrepare(war, null);
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
        return PortProviderUtil.generateURL(path, ResourceConstructorParamsTest.class.getSimpleName());
    }

    @Test
    @DisplayName("Mixed Test")
    public void mixedTest() {
        Response response = client.target(generateURL("/mixed/get")).request().get();
        Assertions.assertEquals(500, response.getStatus(), "Incorrect status code");
    }

    @Test
    @DisplayName("No Params Test")
    public void noParamsTest() {
        Response response = client.target(generateURL("/noparams/get")).request().get();
        Assertions.assertEquals(500, response.getStatus(), "Incorrect status code");
    }

    @Test
    @DisplayName("Params 404 Test")
    public void params404Test() {
        Response response = client.target(generateURL("/params404/get")).queryParam("queryP", "A").request().get();
        Assertions.assertEquals(404, response.getStatus(), "Incorrect status code");
    }

    @Test
    @DisplayName("Params 400 Test")
    public void params400Test() {
        Response response = client.target(generateURL("/params400/get")).request().cookie("cookieP", "A").get();
        Assertions.assertEquals(400, response.getStatus(), "Incorrect status code");
    }

    @Test
    @DisplayName("Params Cookie Web Application Exception Test")
    public void paramsCookieWebApplicationExceptionTest() {
        Response response = client.target(generateURL("/paramsWAECookie/get")).request().cookie("cookieP", "A").get();
        // 405 is just a random one to verify WebApplicationException is not wrapped with NotFoundException.
        Assertions.assertEquals(405, response.getStatus(), "Incorrect status code");
    }

    @Test
    @DisplayName("Params Query Web Application Exception Test")
    public void paramsQueryWebApplicationExceptionTest() {
        Response response = client.target(generateURL("/paramsWAEQuery/get")).queryParam("queryP", "A").request().get();
        // 405 is just a random one to verify WebApplicationException is not wrapped with NotFoundException.
        Assertions.assertEquals(405, response.getStatus(), "Incorrect status code");
    }
}
