package io.quarkus.rest.test.client;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.client.resource.IndirectInvocationTestResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @author <a href="mailto:kanovotn@redhat.com">Katerina Novotna</a>
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 *
 */
public class IndirectInvocationTest extends ClientTestBase {

    public static final int REPEAT = 15;

    Client client;

    @Before
    public void before() {
        client = ClientBuilder.newClient();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, IndirectInvocationTestResource.class);
                }
            });

    @After
    public void close() {
        client.close();
    }

    /**
     * @tpTestDetails Create Invocation request and submit it using invoke() method, verify the answer
     * @tpPassCrit Expected response is returned from the server
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void invokeLaterTest() {
        Invocation inv = client.target(generateURL("/") + "test/query")
                .queryParam("param", "123456")
                .queryParam("id", "3")
                .request("text/plain").buildGet();

        Response response = inv.invoke();
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals("123456 3", response.readEntity(String.class));
    }

    /**
     * @tpTestDetails Create two Invocations requests, store them in the list and then call them multiple times
     * @tpPassCrit Expected response is returned from the server
     * @tpInfo https://weblogs.java.net/blog/spericas/archive/2011/10/20/jax-rs-20-client-api-generic-interface
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void invokeMultipleTimes() {
        Invocation inv1 = client.target(generateURL("/") + "test/query")
                .queryParam("param", "123456")
                .queryParam("id", "3")
                .request("text/plain").buildGet();

        Invocation inv2 = client.target(generateURL("/") + "test/send")
                .queryParam("param", "123456")
                .queryParam("id", "3")
                .request("text/plain").buildPost(Entity.text("50.0"));

        Collection<Invocation> invs = Arrays.asList(inv1, inv2);

        for (int i = 0; i < REPEAT; i++) {
            for (Invocation inv : invs) {
                Response response = inv.invoke();
                Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
                response.close();
            }
        }
    }

}
