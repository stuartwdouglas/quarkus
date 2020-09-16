package io.quarkus.rest.test.core.interceptors;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.function.Supplier;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Variant;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ProxyBuilder;
import org.jboss.resteasy.plugins.interceptors.AcceptEncodingGZIPFilter;
import org.jboss.resteasy.plugins.interceptors.GZIPDecodingInterceptor;
import org.jboss.resteasy.plugins.interceptors.GZIPEncodingInterceptor;
import org.jboss.resteasy.util.ReadFromStream;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.core.interceptors.resource.GzipIGZIP;
import io.quarkus.rest.test.core.interceptors.resource.GzipProxy;
import io.quarkus.rest.test.core.interceptors.resource.GzipResource;
import io.quarkus.rest.test.core.interceptors.resource.Pair;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Interceptors
 * @tpChapter Integration tests
 * @tpTestCaseDetails Gzip compression tests
 * @tpSince RESTEasy 3.0.16
 */
public class GzipTest {

    static QuarkusRestClient client;
    protected static final Logger logger = Logger.getLogger(GzipTest.class.getName());

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(GzipIGZIP.class, Pair.class);
                    // Activate gzip compression:
                    war.addAsManifestResource("org/jboss/resteasy/test/client/javax.ws.rs.ext.Providers",
                            "services/javax.ws.rs.ext.Providers");
                    return TestUtil.finishContainerPrepare(war, null, GzipResource.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, GzipTest.class.getSimpleName());
    }

    @Before
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newBuilder()
                .register(AcceptEncodingGZIPFilter.class)
                .register(GZIPDecodingInterceptor.class)
                .register(GZIPEncodingInterceptor.class)
                .build();
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Check ByteArrayOutputStream of gzip data
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testRawStreams() throws Exception {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        GZIPEncodingInterceptor.EndableGZIPOutputStream outputStream = new GZIPEncodingInterceptor.EndableGZIPOutputStream(
                byteStream);
        outputStream.write("hello world".getBytes());
        outputStream.finish();
        outputStream.close();

        byte[] bytes1 = byteStream.toByteArray();
        logger.info("Output stream length: " + bytes1.length);
        logger.info("Output stream value:" + new String(bytes1));
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes1);
        GZIPDecodingInterceptor.FinishableGZIPInputStream is = new GZIPDecodingInterceptor.FinishableGZIPInputStream(bis,
                false);
        byte[] bytes = ReadFromStream.readFromStream(1024, is);
        is.finish();
        String str = new String(bytes);
        Assert.assertEquals("Output stream has wrong content", "hello world", str);

    }

    /**
     * @tpTestDetails Check ProxyFactory
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testProxy() throws Exception {
        GzipIGZIP proxy = client.target(generateURL("")).proxy(GzipIGZIP.class);
        Assert.assertEquals("Proxy return wrong content", "HELLO WORLD", proxy.getText());
        Assert.assertEquals("Proxy return wrong content", "HELLO WORLD", proxy.getGzipText());

        // resteasy-651
        try {
            proxy.getGzipErrorText();
            Assert.fail("Proxy is unreachable");
        } catch (InternalServerErrorException failure) {
            Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), failure.getResponse().getStatus());
            String txt = failure.getResponse().readEntity(String.class);
            Assert.assertEquals("Response contain wrong content", "Hello", txt);
        }
    }

    /**
     * @tpTestDetails Check length of content. Gzip data should have at least 11 bytes
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testContentLength() throws Exception {
        {
            Response response = client.target(generateURL("/text")).request().get();
            Assert.assertEquals("Response has wrong content", "HELLO WORLD", response.readEntity(String.class));
            String cl = response.getHeaderString("Content-Length");
            if (cl != null) {
                // make sure the content length is greater than 11 because this will be a gzipped encoding
                Assert.assertTrue("Content length should be greater than 11 because this will be a gzipped encoding",
                        Integer.parseInt(cl) > 11);
            }
        }
        {
            Response response = client.target(generateURL("/bytes")).request().get();
            String cl = response.getHeaderString("Content-Length");
            if (cl != null) {
                // make sure the content length is greater than 11 because this will be a gzipped encoding
                int integerCl = Integer.parseInt(cl);
                logger.info("Content-Length: " + integerCl);
                Assert.assertTrue("Content length should be greater than 11 because this will be a gzipped encoding",
                        integerCl > 11);
            }
            Assert.assertEquals("Response contains wrong content", "HELLO WORLD", response.readEntity(String.class));
        }
    }

    /**
     * @tpTestDetails Check wrong URL
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testRequestError() throws Exception {
        Response response = client.target(generateURL("/error")).request().get();
        Assert.assertEquals(Status.METHOD_NOT_ALLOWED.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Check stream from PUT request
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testPutStream() throws Exception {
        Response response = client.target(generateURL("/stream")).request().header("Content-Encoding", "gzip")
                .put(Entity.entity("hello world", "text/plain"));
        Assert.assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Check text from PUT request
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testPutText() throws Exception {
        Response response = client.target(generateURL("/text")).request().header("Content-Encoding", "gzip")
                .put(Entity.entity("hello world", "text/plain"));
        Assert.assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Check plain text response
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testRequestPlain() throws Exception {
        Response response = client.target(generateURL("/text")).request().get();
        Assert.assertEquals("HELLO WORLD", response.readEntity(String.class));

    }

    /**
     * @tpTestDetails Check encoded text response
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testRequestEncoded() throws Exception {
        Response response = client.target(generateURL("/encoded/text")).request().get();
        Assert.assertEquals("Response contains wrong content", "HELLO WORLD", response.readEntity(String.class));
    }

    /**
     * @tpTestDetails Test that it was zipped by running it through Apache HTTP Client which does not automatically unzip
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testWasZipped() throws Exception {
        CloseableHttpClient client = HttpClientBuilder.create().disableContentCompression().build();
        {
            HttpGet get = new HttpGet(generateURL("/encoded/text"));
            get.addHeader("Accept-Encoding", "gzip, deflate");
            HttpResponse response = client.execute(get);
            Assert.assertEquals(Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
            Assert.assertEquals("Wrong encoding format", "gzip", response.getFirstHeader("Content-Encoding").getValue());

            // test that it is actually zipped
            String entity = EntityUtils.toString(response.getEntity());
            logger.info("Entity: " + entity);
            Assert.assertNotSame("Wrong entity content", entity, "HELLO WORLD");
        }

        {
            HttpGet get = new HttpGet(generateURL("/text"));
            get.addHeader("Accept-Encoding", "gzip, deflate");
            HttpResponse response = client.execute(get);
            Assert.assertEquals(Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
            Assert.assertEquals("Wrong encoding format", "gzip", response.getFirstHeader("Content-Encoding").getValue());

            // test that it is actually zipped
            String entity = EntityUtils.toString(response.getEntity());
            Assert.assertNotSame("Wrong entity content", entity, "HELLO WORLD");
        }
    }

    /**
     * @tpTestDetails Test that if there is no accept-encoding: gzip header that result isn't encoded
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testWithoutAcceptEncoding() throws Exception {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(generateURL("/encoded/text"));
        HttpResponse response = client.execute(get);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
        Assert.assertNull(response.getFirstHeader("Content-Encoding"));

        // test that it is actually zipped
        String entity = EntityUtils.toString(response.getEntity());
        Assert.assertEquals("Response contains wrong content", entity, "HELLO WORLD");
    }

    /**
     * @tpTestDetails Send POST request with gzip encoded data using @GZIP annotation and client proxy framework
     * @tpInfo RESTEASY-1499
     * @tpSince RESTEasy 3.1.0
     */
    @Test
    public void testGzipPost() {
        GzipProxy gzipProxy = ProxyBuilder.builder(GzipProxy.class, client.target(generateURL(""))).build();
        Pair data = new Pair();
        data.setP1("first");
        data.setP2("second");

        Response response = gzipProxy.post(data);
        Assert.assertEquals("gzip", response.getHeaderString("Content-Encoding"));
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    /**
     * @tpTestDetails Test exceeding default maximum size
     * @tpInfo RESTEASY-1484
     * @tpSince RESTEasy 3.1.0.Final
     */
    @Test
    public void testMaxDefaultSizeSending() throws Exception {
        byte[] b = new byte[10000001];
        Variant variant = new Variant(MediaType.APPLICATION_OCTET_STREAM_TYPE, "", "gzip");
        Response response = client.target(generateURL("/big/send")).request().post(Entity.entity(b, variant));
        Assert.assertEquals(Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode(), response.getStatus());
        String message = response.readEntity(String.class);
        Assert.assertTrue(message.contains("RESTEASY003357"));
        Assert.assertTrue(message.contains("10000000"));
    }
}
