package io.quarkus.rest.test.interceptor;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
@DisplayName("Client Request Filter Registration Test")
public class ClientRequestFilterRegistrationTest extends ClientTestBase {

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(CustomTestApp.class, ClientRequestFilterImpl.class, ClientResource.class);
            return war;
        }
    });

    @BeforeEach
    public void before() {
        client = ClientBuilder.newClient();
    }

    @AfterEach
    public void close() {
        client.close();
    }

    @Test
    @DisplayName("Filter Registered Test")
    public void filterRegisteredTest() throws Exception {
        WebTarget base = client.target(generateURL("/") + "testIt");
        Response response = base.request().get();
        Assertions.assertEquals(456, response.getStatus());
    }
}
