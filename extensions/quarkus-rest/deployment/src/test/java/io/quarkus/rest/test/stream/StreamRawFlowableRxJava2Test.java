package io.quarkus.rest.test.stream;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.rest.test.stream.resource.StreamRawByteArrayMessageBodyReaderWriter;
import io.quarkus.rest.test.stream.resource.StreamRawByteMessageBodyReaderWriter;
import io.quarkus.rest.test.stream.resource.StreamRawCharArrayMessageBodyReaderWriter;
import io.quarkus.rest.test.stream.resource.StreamRawCharMessageBodyReaderWriter;
import io.quarkus.rest.test.stream.resource.StreamRawFlowableRxJava2Resource;
import io.quarkus.rest.test.stream.resource.StreamRawMediaTypes;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Reactive classes
 * @tpChapter Integration tests
 * @tpSince RESTEasy 4.0
 *
 *          These tests check raw streaming.
 */
@DisplayName("Stream Raw Flowable Rx Java 2 Test")
public class StreamRawFlowableRxJava2Test {

    private static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.setManifest(
                    new StringAsset("Manifest-Version: 1.0\n" + "Dependencies: org.jboss.resteasy.resteasy-rxjava2 services"));
            return TestUtil.finishContainerPrepare(war, null, StreamRawFlowableRxJava2Resource.class,
                    StreamRawByteMessageBodyReaderWriter.class, StreamRawByteArrayMessageBodyReaderWriter.class,
                    StreamRawCharMessageBodyReaderWriter.class, StreamRawCharArrayMessageBodyReaderWriter.class);
        }
    });

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, StreamRawFlowableRxJava2Test.class.getSimpleName());
    }

    // ////////////////////////////////////////////////////////////////////////////
    @BeforeAll
    public static void beforeClass() throws Exception {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterAll
    public static void after() throws Exception {
        client.close();
    }

    // ////////////////////////////////////////////////////////////////////////////
    @Test
    @DisplayName("Test Byte")
    public void testByte() throws Exception {
        doByteTest("default");
        doByteTest("false");
        doByteTest("true");
    }

    void doByteTest(String include) {
        Invocation.Builder request = client.target(generateURL("/byte/" + include)).request();
        Response response = request.get();
        StreamRawMediaTypes.testMediaType("byte", include, MediaType.valueOf(response.getHeaderString("Content-Type")));
        byte[] entity = response.readEntity(byte[].class);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(3, entity.length);
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals((byte) i, entity[i]);
        }
    }

    @Test
    @DisplayName("Test Byte Array")
    public void testByteArray() throws Exception {
        doByteArrayTest("default");
        doByteArrayTest("false");
        doByteArrayTest("true");
    }

    void doByteArrayTest(String include) {
        Invocation.Builder request = client.target(generateURL("/bytes/" + include)).request();
        Response response = request.get();
        StreamRawMediaTypes.testMediaType("byte", include, MediaType.valueOf(response.getHeaderString("Content-Type")));
        byte[] entity = response.readEntity(byte[].class);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(9, entity.length);
        byte[] expected = new byte[] { 0, 1, 2, 0, 1, 2, 0, 1, 2 };
        for (int i = 0; i < 9; i++) {
            Assertions.assertEquals(expected[i], entity[i]);
        }
    }

    @Test
    @DisplayName("Test Char")
    public void testChar() throws Exception {
        doCharTest("default");
        doCharTest("false");
        doCharTest("true");
    }

    void doCharTest(String include) {
        Invocation.Builder request = client.target(generateURL("/char/" + include)).request();
        Response response = request.get();
        StreamRawMediaTypes.testMediaType("char", include, MediaType.valueOf(response.getHeaderString("Content-Type")));
        String entity = response.readEntity(String.class);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(entity, "abc");
    }

    @Test
    @DisplayName("Test Char Array")
    public void testCharArray() throws Exception {
        doCharArrayTest("default");
        doCharArrayTest("false");
        doCharArrayTest("true");
    }

    void doCharArrayTest(String include) {
        Invocation.Builder request = client.register(StreamRawCharArrayMessageBodyReaderWriter.class)
                .target(generateURL("/chars/" + include)).request();
        Response response = request.get();
        StreamRawMediaTypes.testMediaType("char", include, MediaType.valueOf(response.getHeaderString("Content-Type")));
        Character[] entity = response.readEntity(Character[].class);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(9, entity.length);
        Character[] chars = new Character[] { 'a', 'b', 'c', 'a', 'b', 'c', 'a', 'b', 'c' };
        for (int i = 0; i < entity.length; i++) {
            Assertions.assertEquals(chars[i], entity[i]);
        }
    }
}
