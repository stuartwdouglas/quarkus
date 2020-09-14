package io.quarkus.rest.test.core.encoding;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.core.encoding.resource.SpecialCharactersProxy;
import io.quarkus.rest.test.core.encoding.resource.SpecialCharactersResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Encoding
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-208 and RESTEASY-214
 * @tpSince RESTEasy 3.0.16
 */
public class SpecialCharactersTest {

    protected static QuarkusRestClient client;

    @Before
    public void setup() throws Exception {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @After
    public void shutdown() throws Exception {
        client.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(SpecialCharactersProxy.class);
                    return TestUtil.finishContainerPrepare(war, null, SpecialCharactersResource.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, SpecialCharactersTest.class.getSimpleName());
    }

    private static final String SPACES_REQUEST = "something something";
    private static final String QUERY = "select p from VirtualMachineEntity p where guest.guestId = :id";

    @Test
    public void testEcho() {
        SpecialCharactersProxy proxy = client.target(generateURL("")).proxy(SpecialCharactersProxy.class);
        Assert.assertEquals(SPACES_REQUEST, proxy.sayHi(SPACES_REQUEST));
        Assert.assertEquals(QUERY, proxy.compile(QUERY));
    }

    @Test
    public void testIt() throws Exception {
        Response response = client.target(generateURL("/sayhello/widget/08%2F26%2F2009")).request().get();
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("Wrong content of response", "08/26/2009", response.readEntity(String.class));
        response.close();
    }

    @Test
    public void testPlus() throws Exception {
        Response response = client.target(generateURL("/sayhello/plus/foo+bar")).request().get();
        Assert.assertEquals(Status.OK, response.getStatus());
        // assert is in resource
        response.close();
    }

    @Test
    public void testPlus2() throws Exception {
        Response response = client.target(generateURL("/sayhello/plus/foo+bar")).request().get();
        Assert.assertEquals(Status.OK, response.getStatus());
        // assert is in resource
        response.close();
    }
}
