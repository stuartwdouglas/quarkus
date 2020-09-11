package io.quarkus.rest.test.resource.constructor;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
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
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import io.quarkus.rest.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.rest.test.simple.TestUtil;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;


public class ResourceConstructorParamsTest {
    protected static final Logger logger = Logger.getLogger(
            ResourceConstructorParamsTest.class.getName());

    static QuarkusRestClient client;

     @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
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
    }});

    @Before
    public void init() {
        client = (QuarkusRestClient)ClientBuilder.newClient();
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path,
                ResourceConstructorParamsTest.class.getSimpleName());
    }

    @Test
    public void mixedTest() {
        Response response = client.target(generateURL("/mixed/get"))
                .request()
                .get();
        Assert.assertEquals("Incorrect status code", 500,
                response.getStatus());
    }

    @Test
    public void noParamsTest() {
        Response response = client.target(generateURL("/noparams/get"))
                .request()
                .get();
        Assert.assertEquals("Incorrect status code", 500,
                response.getStatus());
    }

    @Test
    public void params404Test() {
        Response response = client.target(generateURL("/params404/get"))
                .queryParam("queryP", "A")
                .request()
                .get();
        Assert.assertEquals("Incorrect status code", 404,
                response.getStatus());
    }

    @Test
    public void params400Test() {
        Response response = client.target(generateURL("/params400/get"))
                .request()
                .cookie("cookieP", "A")
                .get();
        Assert.assertEquals("Incorrect status code", 400,
                response.getStatus());
    }

    @Test
    public void paramsCookieWebApplicationExceptionTest() {
        Response response = client.target(generateURL("/paramsWAECookie/get"))
                .request()
                .cookie("cookieP", "A")
                .get();

        // 405 is just a random one to verify WebApplicationException is not wrapped with NotFoundException.
        Assert.assertEquals("Incorrect status code", 405,
                response.getStatus());
    }

    @Test
    public void paramsQueryWebApplicationExceptionTest() {
        Response response = client.target(generateURL("/paramsWAEQuery/get"))
                .queryParam("queryP", "A")
                .request()
                .get();

        // 405 is just a random one to verify WebApplicationException is not wrapped with NotFoundException.
        Assert.assertEquals("Incorrect status code", 405,
                response.getStatus());
    }
}
