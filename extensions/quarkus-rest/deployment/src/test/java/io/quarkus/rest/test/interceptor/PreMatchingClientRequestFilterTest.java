package io.quarkus.rest.test.interceptor;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;


import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.rules.ExpectedException;

import io.quarkus.rest.test.client.ClientTestBase;
import io.quarkus.rest.test.interceptor.resource.PreMatchingClientRequestFilterImpl;
import io.quarkus.rest.test.interceptor.resource.PreMatchingClientResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Interceptor
 * @tpChapter Integration tests
 * @tpTestCaseDetails Tests @PreMatching annotation on ClientRequestFilter (RESTEASY-1696)
 * @tpSince RESTEasy 4.0.0
 */
@DisplayName("Pre Matching Client Request Filter Test")
public class PreMatchingClientRequestFilterTest extends ClientTestBase {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            // rls //war.addClass(ClientExceptionsData.class);
            return TestUtil.finishContainerPrepare(war, null, PreMatchingClientResource.class);
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

    /**
     * @tpTestDetails Test that annotation @PreMatching on an implementation of ClientRequestFilter
     *                is ignored. This annotation is only valid on ContainerRequestFilter implementations.
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    @DisplayName("Pre Matching Test")
    public void preMatchingTest() throws Exception {
        WebTarget base = client.target(generateURL("/") + "testIt");
        Response response = base.register(PreMatchingClientRequestFilterImpl.class).request().get();
        Assertions.assertEquals(404, response.getStatus());
    }
}
