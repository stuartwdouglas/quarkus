package io.quarkus.rest.test.core.encoding;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
@DisplayName("Special Characters Test")
public class SpecialCharactersTest {

    protected static QuarkusRestClient client;

    @BeforeEach
    public void setup() throws Exception {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void shutdown() throws Exception {
        client.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

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
    @DisplayName("Test Echo")
    public void testEcho() {
        SpecialCharactersProxy proxy = client.target(generateURL("")).proxy(SpecialCharactersProxy.class);
        Assertions.assertEquals(SPACES_REQUEST, proxy.sayHi(SPACES_REQUEST));
        Assertions.assertEquals(QUERY, proxy.compile(QUERY));
    }

    @Test
    @DisplayName("Test It")
    public void testIt() throws Exception {
        Response response = client.target(generateURL("/sayhello/widget/08%2F26%2F2009")).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("08/26/2009", response.readEntity(String.class), "Wrong content of response");
        response.close();
    }

    @Test
    @DisplayName("Test Plus")
    public void testPlus() throws Exception {
        Response response = client.target(generateURL("/sayhello/plus/foo+bar")).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // assert is in resource
        response.close();
    }

    @Test
    @DisplayName("Test Plus 2")
    public void testPlus2() throws Exception {
        Response response = client.target(generateURL("/sayhello/plus/foo+bar")).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // assert is in resource
        response.close();
    }
}
