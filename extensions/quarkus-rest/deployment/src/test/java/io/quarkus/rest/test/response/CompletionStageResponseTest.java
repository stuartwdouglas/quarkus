package io.quarkus.rest.test.response;

import java.net.InetAddress;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
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
import io.quarkus.rest.test.response.resource.AsyncResponseCallback;
import io.quarkus.rest.test.response.resource.CompletionStageProxy;
import io.quarkus.rest.test.response.resource.CompletionStageResponseMessageBodyWriter;
import io.quarkus.rest.test.response.resource.CompletionStageResponseResource;
import io.quarkus.rest.test.response.resource.CompletionStageResponseTestClass;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter CompletionStage response type
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.5
 */
@DisplayName("Completion Stage Response Test")
public class CompletionStageResponseTest {

    static boolean serverIsLocal;

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(CompletionStageResponseTestClass.class);
            war.addClass(CompletionStageProxy.class);
            war.setManifest(new StringAsset("Manifest-Version: 1.0\n"
                    + "Dependencies: org.jboss.resteasy.resteasy-rxjava2 services, org.reactivestreams\n"));
            return TestUtil.finishContainerPrepare(war, null, CompletionStageResponseMessageBodyWriter.class,
                    CompletionStageResponseResource.class, SingleProvider.class, AsyncResponseCallback.class);
        }
    });

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, CompletionStageResponseTest.class.getSimpleName());
    }

    @BeforeAll
    public static void setup() throws Exception {
        client = (QuarkusRestClient) ClientBuilder.newClient();
        // Undertow's default behavior is to send an HTML error page only if the client and
        // server are communicating on a loopback connection. Otherwise, it returns "".
        Invocation.Builder request = client.target(generateURL("/host")).request();
        Response response = request.get();
        String host = response.readEntity(String.class);
        InetAddress addr = InetAddress.getByName(host);
        serverIsLocal = addr.isLoopbackAddress();
    }

    @AfterAll
    public static void close() {
        client.close();
        client = null;
    }

    /**
     * @tpTestDetails Resource method returns CompletionStage<String>.
     * @tpSince RESTEasy 3.5
     */
    @Test
    @DisplayName("Test Text")
    public void testText() throws Exception {
        Invocation.Builder request = client.target(generateURL("/text")).request();
        Response response = request.get();
        String entity = response.readEntity(String.class);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(CompletionStageResponseResource.HELLO, entity);
        // make sure the completion callback was called with no error
        request = client.target(generateURL("/callback-called-no-error?p=text")).request();
        response = request.get();
        Assertions.assertEquals(200, response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Resource method returns CompletionStage<Response>.
     *                Response has MediaType "text/plain" overriding @Produces("text/xxx").
     * @tpSince RESTEasy 3.5
     */
    @Test
    @DisplayName("Test Response")
    public void testResponse() throws Exception {
        Invocation.Builder request = client.target(generateURL("/response")).request();
        Response response = request.get();
        String entity = response.readEntity(String.class);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(response.getHeaderString("Content-Type"), "text/plain;charset=UTF-8");
        Assertions.assertEquals(CompletionStageResponseResource.HELLO, entity);
    }

    /**
     * @tpTestDetails Resource method returns CompletionStage<CompletionStageResponseTestClass>,
     *                where CompletionStageResponseTestClass is handled by CompletionStageResponseMessageBodyWriter,
     *                which has annotation @Produces("abc/xyz").
     * @tpSince RESTEasy 3.5
     */
    @Test
    @DisplayName("Test Test Class")
    public void testTestClass() throws Exception {
        Invocation.Builder request = client.target(generateURL("/testclass")).request();
        Response response = request.get();
        String entity = response.readEntity(String.class);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(response.getHeaderString("Content-Type"), "abc/xyz");
        Assertions.assertEquals(entity, "pdq");
    }

    /**
     * @tpTestDetails Resource method returns CompletionStage<Response>, where the Response
     *                emtity is a CompletionStageResponseTestClass, and where
     *                CompletionStageResponseTestClass is handled by CompletionStageResponseMessageBodyWriter,
     *                which has annotation @Produces("abc/xyz").
     * @tpSince RESTEasy 3.5
     */
    @Test
    @DisplayName("Test Response Test Class")
    public void testResponseTestClass() throws Exception {
        Invocation.Builder request = client.target(generateURL("/responsetestclass")).request();
        Response response = request.get();
        String entity = response.readEntity(String.class);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(response.getHeaderString("Content-Type"), "abc/xyz");
        Assertions.assertEquals(entity, "pdq");
    }

    /**
     * @tpTestDetails Resource method return type is CompletionStage<String>, and it passes
     *                null to CompleteableFuture.complete().
     * @tpSince RESTEasy 3.5
     */
    @Test
    @DisplayName("Test Null")
    public void testNull() throws Exception {
        Invocation.Builder request = client.target(generateURL("/null")).request();
        Response response = request.get();
        String entity = response.readEntity(String.class);
        Assertions.assertEquals(204, response.getStatus());
        Assertions.assertEquals(null, entity);
    }

    /**
     * @tpTestDetails Resource method passes a WebApplicationException to
     *                to CompleteableFuture.completeExceptionally().
     * @tpSince RESTEasy 3.5
     */
    @Test
    @DisplayName("Test Exception Delay")
    public void testExceptionDelay() throws Exception {
        Invocation.Builder request = client.target(generateURL("/exception/delay")).request();
        Response response = request.get();
        String entity = response.readEntity(String.class);
        Assertions.assertEquals(444, response.getStatus());
        Assertions.assertEquals(CompletionStageResponseResource.EXCEPTION, entity);
        // make sure the completion callback was called with with an error
        request = client.target(generateURL("/callback-called-with-error?p=exception/delay")).request();
        response = request.get();
        Assertions.assertEquals(200, response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Resource method throws a WebApplicationException in a CompletionStage
     *                pipeline
     * @tpSince RESTEasy 3.5
     */
    @Test
    @DisplayName("Test Exception Delay Wrapped")
    public void testExceptionDelayWrapped() throws Exception {
        Invocation.Builder request = client.target(generateURL("/exception/delay-wrapped")).request();
        Response response = request.get();
        String entity = response.readEntity(String.class);
        Assertions.assertEquals(444, response.getStatus());
        Assertions.assertEquals(CompletionStageResponseResource.EXCEPTION, entity);
        // make sure the completion callback was called with with an error
        request = client.target(generateURL("/callback-called-with-error?p=exception/delay-wrapped")).request();
        response = request.get();
        Assertions.assertEquals(200, response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Resource method return type is CompletionStage<String>, but it
     *                throws a RuntimeException without creating a CompletionStage.
     * @tpSince RESTEasy 3.5
     */
    @Test
    @DisplayName("Test Exception Immediate Runtime")
    public void testExceptionImmediateRuntime() throws Exception {
        Invocation.Builder request = client.target(generateURL("/exception/immediate/runtime")).request();
        Response response = request.get();
        String entity = response.readEntity(String.class);
        Assertions.assertEquals(500, response.getStatus());
        response.close();
        // make sure the completion callback was called with with an error
        request = client.target(generateURL("/callback-called-with-error?p=exception/immediate/runtime")).request();
        response = request.get();
        Assertions.assertEquals(200, response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Resource method return type is CompletionStage<String>, but it
     *                throws an Exception without creating a CompletionStage.
     * @tpSince RESTEasy 3.5
     */
    @Test
    @DisplayName("Test Exception Immediate Not Runtime")
    public void testExceptionImmediateNotRuntime() throws Exception {
        Invocation.Builder request = client.target(generateURL("/exception/immediate/notruntime")).request();
        Response response = request.get();
        String entity = response.readEntity(String.class);
        Assertions.assertEquals(500, response.getStatus());
        response.close();
        // make sure the completion callback was called with with an error
        request = client.target(generateURL("/callback-called-with-error?p=exception/immediate/notruntime")).request();
        response = request.get();
        Assertions.assertEquals(200, response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Resource method returns CompletionStage<String>.
     * @tpSince RESTEasy 3.5
     */
    @Test
    @DisplayName("Test Text Single")
    public void testTextSingle() throws Exception {
        Invocation.Builder request = client.target(generateURL("/textSingle")).request();
        Response response = request.get();
        String entity = response.readEntity(String.class);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(CompletionStageResponseResource.HELLO, entity);
    }

    /**
     * @tpTestDetails Resource method returns CompletionStage<String>, data are computed after end-point method ends
     * @tpSince RESTEasy 3.5
     */
    @Test
    @DisplayName("Get Data With Delay Test")
    public void getDataWithDelayTest() throws Exception {
        Invocation.Builder request = client.target(generateURL("/sleep")).request();
        Future<Response> future = request.async().get();
        Assertions.assertFalse(future.isDone());
        Response response = future.get();
        String entity = response.readEntity(String.class);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(CompletionStageResponseResource.HELLO, entity);
    }

    /**
     * @tpTestDetails Resource method returns CompletionStage<String>, client try to use proxy
     *                Regression check for https://issues.jboss.org/browse/RESTEASY-1798
     *                - RESTEasy proxy client can't use RxClient and CompletionStage
     * @tpSince RESTEasy 3.5
     */
    @Test
    @DisplayName("Proxy Test")
    public void proxyTest() throws Exception {
        CompletionStageProxy proxy = client.target(generateURL("/")).proxy(CompletionStageProxy.class);
        Future<String> future = proxy.sleep().toCompletableFuture();
        Assertions.assertFalse(future.isDone());
        Assertions.assertEquals(CompletionStageResponseResource.HELLO, future.get());
    }
}
