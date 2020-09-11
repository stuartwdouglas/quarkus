package io.quarkus.rest.test.interceptor;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.client.ClientTestBase;
import io.quarkus.rest.test.interceptor.resource.ClientRequestFilterImpl;
import io.quarkus.rest.test.interceptor.resource.ClientResource;
import io.quarkus.rest.test.interceptor.resource.CustomTestApp;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Interceptor
 * @tpChapter Integration tests
 * @tpTestCaseDetails Tests @Provider annotation on ClientRequestFilter (RESTEASY-2084)
 * @tpSince RESTEasy 4.0.0
 */
public class ClientRequestFilterRegistrationTest extends ClientTestBase {

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(CustomTestApp.class, ClientRequestFilterImpl.class, ClientResource.class);
                    return war;
                }
            });

    @Before
    public void before() {
        client = ClientBuilder.newClient();
    }

    @After
    public void close() {
        client.close();
    }

    @Test
    public void filterRegisteredTest() throws Exception {
        WebTarget base = client.target(generateURL("/") + "testIt");
        Response response = base.request().get();
        Assert.assertEquals(456, response.getStatus());
    }

}
