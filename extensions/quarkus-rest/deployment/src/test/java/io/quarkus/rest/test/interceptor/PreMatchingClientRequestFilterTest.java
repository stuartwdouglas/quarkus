package io.quarkus.rest.test.interceptor;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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
public class PreMatchingClientRequestFilterTest extends ClientTestBase {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    //rls //war.addClass(ClientExceptionsData.class);
                    return TestUtil.finishContainerPrepare(war, null, PreMatchingClientResource.class);
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

    /**
     * @tpTestDetails Test that annotation @PreMatching on an implementation of ClientRequestFilter
     *                is ignored. This annotation is only valid on ContainerRequestFilter implementations.
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void preMatchingTest() throws Exception {
        WebTarget base = client.target(generateURL("/") + "testIt");
        Response response = base.register(PreMatchingClientRequestFilterImpl.class).request().get();
        Assert.assertEquals(404, response.getStatus());
    }

}
