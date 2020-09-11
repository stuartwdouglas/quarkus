package io.quarkus.rest.test.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.ProxyBuilder;
import org.jboss.resteasy.util.ReadFromStream;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
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
public class InputStreamResourceTest extends ClientTestBase {

    static Client QuarkusRestClient;

    @BeforeClass
    public static void setup() {
        QuarkusRestClient = ClientBuilder.newClient();
    }

    @AfterClass
    public static void close() {
        QuarkusRestClient.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
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
    public void testClientResponse() throws Exception {
        InputStreamResourceClient client = ProxyBuilder
                .builder(InputStreamResourceClient.class, QuarkusRestClient.target(generateURL(""))).build();
        Assert.assertEquals("hello", client.getAsString());
        Response is = client.getAsInputStream();
        Assert.assertEquals("hello", new String(ReadFromStream.readFromStream(1024, is.readEntity(InputStream.class))));
        is.close();
        client.postString("new value");
        Assert.assertEquals("new value", client.getAsString());
        client.postInputStream(new ByteArrayInputStream("new value 2".getBytes()));
        Assert.assertEquals("new value 2", client.getAsString());
    }
}
