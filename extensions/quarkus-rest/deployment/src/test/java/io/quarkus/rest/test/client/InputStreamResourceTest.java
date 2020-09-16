package io.quarkus.rest.test.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.ProxyBuilder;
import org.jboss.resteasy.util.ReadFromStream;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.client.resource.InputStreamResourceClient;
import io.quarkus.rest.test.client.resource.InputStreamResourceService;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpTestCaseDetails Read and write InputStreams
 * @tpSince RESTEasy 3.0.20
 */
@DisplayName("Input Stream Resource Test")
public class InputStreamResourceTest extends ClientTestBase {

    static Client QuarkusRestClient;

    @BeforeAll
    public static void setup() {
        QuarkusRestClient = ClientBuilder.newClient();
    }

    @AfterAll
    public static void close() {
        QuarkusRestClient.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, InputStreamResourceService.class);
        }
    });

    /**
     * @tpTestDetails Read Strings as either Strings or InputStreams
     * @tpSince RESTEasy 3.0.20
     */
    @Test
    @DisplayName("Test Client Response")
    public void testClientResponse() throws Exception {
        InputStreamResourceClient client = ProxyBuilder
                .builder(InputStreamResourceClient.class, QuarkusRestClient.target(generateURL(""))).build();
        Assertions.assertEquals(client.getAsString(), "hello");
        Response is = client.getAsInputStream();
        Assertions.assertEquals(new String(ReadFromStream.readFromStream(1024, is.readEntity(InputStream.class))), "hello");
        is.close();
        client.postString("new value");
        Assertions.assertEquals(client.getAsString(), "new value");
        client.postInputStream(new ByteArrayInputStream("new value 2".getBytes()));
        Assertions.assertEquals(client.getAsString(), "new value 2");
    }
}
