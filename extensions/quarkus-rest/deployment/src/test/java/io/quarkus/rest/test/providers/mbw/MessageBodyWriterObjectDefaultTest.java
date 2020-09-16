package io.quarkus.rest.test.providers.mbw;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.providers.mbw.resource.MessageBodyWriterObjectMessage;
import io.quarkus.rest.test.providers.mbw.resource.MessageBodyWriterObjectMessageBodyWriter;
import io.quarkus.rest.test.providers.mbw.resource.MessageBodyWriterObjectResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy MessageBodyWriter<Object>
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.1.4
 */
@DisplayName("Message Body Writer Object Default Test")
public class MessageBodyWriterObjectDefaultTest {

    static Client client;

    @BeforeAll
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(MessageBodyWriterObjectMessage.class);
            return TestUtil.finishContainerPrepare(war, null, MessageBodyWriterObjectResource.class,
                    MessageBodyWriterObjectMessageBodyWriter.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, MessageBodyWriterObjectDefaultTest.class.getSimpleName());
    }

    @AfterAll
    public static void close() {
        client.close();
    }

    @Test
    @DisplayName("Test Default")
    public void testDefault() throws Exception {
        Invocation.Builder request = client.target(generateURL("/test")).request();
        Response response = request.get();
        String entity = response.readEntity(String.class);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(entity, "ok");
        Assertions.assertEquals(response.getHeaderString("Content-Type"), "xx/yy");
        request = client.target(generateURL("/test/used")).request();
        response = request.get();
        Assertions.assertTrue(Boolean.parseBoolean(response.readEntity(String.class)));
    }

    @Test
    @DisplayName("Test Get Boolean")
    public // RESTEASY-1730: Could not find MessageBodyWriter for response object of type: java.lang.Boolean of media type: application/octet-stream
    void testGetBoolean() throws Exception {
        Invocation.Builder request = client.target(generateURL("/test/getbool")).request();
        Response response = request.get();
        String entity = response.readEntity(String.class);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(entity, "true");
    }
}
