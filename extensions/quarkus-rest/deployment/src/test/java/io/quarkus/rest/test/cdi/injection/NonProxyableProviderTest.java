package io.quarkus.rest.test.cdi.injection;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.quarkus.rest.test.cdi.injection.resource.FinalMethodSuperclass;
import io.quarkus.rest.test.cdi.injection.resource.NonProxyableProviderResource;
import io.quarkus.rest.test.cdi.injection.resource.ProviderFinalClassStringHandler;
import io.quarkus.rest.test.cdi.injection.resource.ProviderFinalClassStringHandlerBodyWriter;
import io.quarkus.rest.test.cdi.injection.resource.ProviderFinalInheritedMethodStringHandler;
import io.quarkus.rest.test.cdi.injection.resource.ProviderFinalInheritedMethodStringHandlerBodyWriter;
import io.quarkus.rest.test.cdi.injection.resource.ProviderOneArgConstructorStringHandler;
import io.quarkus.rest.test.cdi.injection.resource.ProviderOneArgConstructorStringHandlerBodyWriter;
import io.quarkus.rest.test.simple.PortProviderUtil;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for RESTEASY-1015
 *                    Test that proxy class is not created for provider class
 *                    that cannot be proxied.
 * @tpSince RESTEasy 3.7
 */
public class NonProxyableProviderTest {

    protected static final Logger logger = LogManager.getLogger(
            NonProxyableProviderTest.class.getName());

    Client client;

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "SingleLibrary.jar");
        jar.addClasses(ProviderFinalClassStringHandler.class,
                ProviderFinalClassStringHandlerBodyWriter.class,
                ProviderFinalInheritedMethodStringHandler.class,
                ProviderFinalInheritedMethodStringHandlerBodyWriter.class,
                FinalMethodSuperclass.class,
                ProviderOneArgConstructorStringHandler.class,
                ProviderOneArgConstructorStringHandlerBodyWriter.class);

        WebArchive war = ShrinkWrap.create(WebArchive.class,
                NonProxyableProviderTest.class.getSimpleName() + ".war");
        war.addClass(NonProxyableProviderResource.class);
        war.addAsWebInfResource(
                NonProxyableProviderTest.class.getPackage(),
                "ProviderFinalClass_web.xml", "web.xml");
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        war.addAsLibrary(jar);
        return war;
    }

    @Before
    public void init() {
        client = ClientBuilder.newClient();
    }

    @After
    public void close() {
        client.close();
    }

    /**
     * @tpTestDetails Test CDI does not create proxy class for provider bean declared final
     * @tpSince RESTEasy 3.7
     */
    @Test
    public void testFinalProvider() throws Exception {
        test("a");
    }

    /**
     * @tpTestDetails Test CDI does not create proxy class for provider bean with an inherited final method
     * @tpSince RESTEasy 3.7
     */
    @Test
    public void testInheritedFinalMethodProvider() throws Exception {
        test("b");
    }

    /**
     * @tpTestDetails Test CDI does not create proxy class for provider bean without a non-private no-arg constructor
     * @tpSince RESTEasy 3.7
     */
    @Test
    public void testOneArgConstructorProvider() throws Exception {
        test("c");
    }

    private void test(String subpath) {
        String url = PortProviderUtil.generateURL("/new/" + subpath,
                NonProxyableProviderTest.class.getSimpleName());
        WebTarget base = client.target(url);

        Response response = base.request().get();
        assertEquals(Status.OK, response.getStatus());
        response.close();
    }
}
