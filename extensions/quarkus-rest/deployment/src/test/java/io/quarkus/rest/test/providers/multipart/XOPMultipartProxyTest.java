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

import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

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
public class XOPMultipartProxyTest {

    private static Client client;
    private static XOPMultipartProxy proxy;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
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

    @BeforeClass
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
        ResteasyWebTarget target = (ResteasyWebTarget) client.target(generateURL(""));
        proxy = target.proxy(XOPMultipartProxy.class);
    }

    @AfterClass
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
    public void testXOPGet() throws Exception {
        ResteasyWebTarget target = (ResteasyWebTarget) client.target(generateURL(""));
        XOPMultipartProxy test = target.proxy(XOPMultipartProxy.class);
        XOPMultipartProxyGetFileResponse fileResp = test.getFile("testXOPGet");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        fileResp.getData().writeTo(out);
        Assert.assertEquals("testXOPGet", new String(out.toByteArray()));
    }

    /**
     * @tpTestDetails Send XOP message
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testXOPSend() throws Exception {
        File tmpFile = File.createTempFile("pre", ".tmp");
        tmpFile.deleteOnExit();
        Writer writer = new FileWriter(tmpFile);
        writer.write("testXOPSend");
        writer.close();
        XOPMultipartProxyPutFileRequest req = new XOPMultipartProxyPutFileRequest();
        req.setContent(new DataHandler(new FileDataSource(tmpFile)));
        Response response = proxy.putFile(req);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("testXOPSend", response.readEntity(String.class));
    }
}
