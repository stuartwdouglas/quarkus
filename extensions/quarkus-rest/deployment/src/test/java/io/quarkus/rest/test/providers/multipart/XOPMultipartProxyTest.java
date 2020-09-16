package io.quarkus.rest.test.providers.multipart;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.function.Supplier;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.providers.multipart.resource.XOPMultipartProxy;
import io.quarkus.rest.test.providers.multipart.resource.XOPMultipartProxyGetFileResponse;
import io.quarkus.rest.test.providers.multipart.resource.XOPMultipartProxyPutFileRequest;
import io.quarkus.rest.test.providers.multipart.resource.XOPMultipartProxyResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Multipart provider used to send and receive XOP messages. RESTEASY-2127.
 * @tpChapter Integration tests
 * @tpSince RESTEasy 4.0.0
 */
@DisplayName("Xop Multipart Proxy Test")
public class XOPMultipartProxyTest {

    private static Client client;

    private static XOPMultipartProxy proxy;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(XOPMultipartProxyGetFileResponse.class);
            war.addClass(XOPMultipartProxyPutFileRequest.class);
            war.addClass(XOPMultipartProxy.class);
            return TestUtil.finishContainerPrepare(war, null, XOPMultipartProxyResource.class);
        }
    });

    @BeforeAll
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
        QuarkusRestWebTarget target = (QuarkusRestWebTarget) client.target(generateURL(""));
        proxy = target.proxy(XOPMultipartProxy.class);
    }

    @AfterAll
    public static void after() throws Exception {
        client.close();
    }

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, XOPMultipartProxyTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Receive XOP message
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    @DisplayName("Test XOP Get")
    public void testXOPGet() throws Exception {
        QuarkusRestWebTarget target = (QuarkusRestWebTarget) client.target(generateURL(""));
        XOPMultipartProxy test = target.proxy(XOPMultipartProxy.class);
        XOPMultipartProxyGetFileResponse fileResp = test.getFile("testXOPGet");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        fileResp.getData().writeTo(out);
        Assertions.assertEquals(new String(out.toByteArray()), "testXOPGet");
    }

    /**
     * @tpTestDetails Send XOP message
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    @DisplayName("Test XOP Send")
    public void testXOPSend() throws Exception {
        File tmpFile = File.createTempFile("pre", ".tmp");
        tmpFile.deleteOnExit();
        Writer writer = new FileWriter(tmpFile);
        writer.write("testXOPSend");
        writer.close();
        XOPMultipartProxyPutFileRequest req = new XOPMultipartProxyPutFileRequest();
        req.setContent(new DataHandler(new FileDataSource(tmpFile)));
        Response response = proxy.putFile(req);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "testXOPSend");
    }
}
