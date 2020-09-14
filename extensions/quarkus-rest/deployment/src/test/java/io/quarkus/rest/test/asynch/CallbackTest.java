package io.quarkus.rest.test.asynch;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClientBuilder;
import io.quarkus.rest.test.asynch.resource.CallbackExceptionThrowingStringBean;
import io.quarkus.rest.test.asynch.resource.CallbackResource;
import io.quarkus.rest.test.asynch.resource.CallbackResourceBase;
import io.quarkus.rest.test.asynch.resource.CallbackSecondSettingCompletionCallback;
import io.quarkus.rest.test.asynch.resource.CallbackSettingCompletionCallback;
import io.quarkus.rest.test.asynch.resource.CallbackStringBean;
import io.quarkus.rest.test.asynch.resource.CallbackStringBeanEntityProvider;
import io.quarkus.rest.test.asynch.resource.CallbackTimeoutHandler;
import io.quarkus.rest.test.asynch.resource.JaxrsAsyncServletAsyncResponseBlockingQueue;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Asynchronous RESTEasy
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for async exception handling
 * @tpSince RESTEasy 3.0.16
 */
public class CallbackTest {
    public static Client client;

    @BeforeClass
    public static void initClient() {
        client = ((QuarkusRestClientBuilder) ClientBuilder.newBuilder()).connectionPoolSize(10).build();
    }

    @AfterClass
    public static void closeClient() {
        client.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(CallbackResource.class,
                            CallbackExceptionThrowingStringBean.class,
                            CallbackTimeoutHandler.class,
                            CallbackResourceBase.class,
                            CallbackSecondSettingCompletionCallback.class,
                            CallbackSettingCompletionCallback.class,
                            CallbackStringBean.class,
                            CallbackStringBeanEntityProvider.class,
                            JaxrsAsyncServletAsyncResponseBlockingQueue.class);
                    war.addAsWebInfResource(AsyncPostProcessingTest.class.getPackage(), "CallbackTestWeb.xml", "web.xml");
                    return TestUtil.finishContainerPrepare(war, null, CallbackResource.class,
                            CallbackStringBeanEntityProvider.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, CallbackTest.class.getSimpleName());
    }

    protected void invokeClear() {
        Response response = client.target(generateURL("/resource/clear")).request().get();
        Assert.assertEquals(Status.NO_CONTENT, response.getStatus());
        response.close();
    }

    protected void invokeReset() {
        Response response = client.target(generateURL("/resource/reset")).request().get();
        Assert.assertEquals(Status.NO_CONTENT, response.getStatus());
        response.close();
    }

    protected void assertString(Future<Response> future, String check) throws Exception {
        Response response = future.get();
        Assert.assertEquals(Status.OK, response.getStatus());
        String entity = response.readEntity(String.class);
        Assert.assertEquals(entity, check);

    }

    /**
     * @tpTestDetails Argument contains exception in two callback classes
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void argumentContainsExceptionInTwoCallbackClassesTest() throws Exception {
        invokeClear();
        invokeReset();
        Future<Response> suspend = client.target(generateURL("/resource/suspend")).request().async().get();

        Future<Response> register = client.target(generateURL("/resource/registerclasses?stage=0")).request().async().get();
        assertString(register, CallbackResourceBase.FALSE);

        Future<Response> exception = client.target(generateURL("/resource/exception?stage=1")).request().async().get();
        Response response = exception.get();
        Assert.assertEquals("Request return wrong response", CallbackResourceBase.TRUE, response.readEntity(String.class));

        Response suspendResponse = suspend.get();
        Assert.assertEquals(suspendResponse.getStatusInfo(), Response.Status.INTERNAL_SERVER_ERROR);
        suspendResponse.close();

        Future<Response> error = client.target(generateURL("/resource/error")).request().async().get();
        assertString(error, RuntimeException.class.getName());
        error = client.target(generateURL("/resource/seconderror")).request().async().get();
        assertString(error, RuntimeException.class.getName());
    }

    /**
     * @tpTestDetails Argument contains exception in two callback instances
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void argumentContainsExceptionInTwoCallbackInstancesTest() throws Exception {
        invokeClear();
        invokeReset();
        Future<Response> suspend = client.target(generateURL("/resource/suspend")).request().async().get();

        Future<Response> register = client.target(generateURL("/resource/registerobjects?stage=0")).request().async().get();
        assertString(register, CallbackResourceBase.FALSE);

        Future<Response> exception = client.target(generateURL("/resource/exception?stage=1")).request().async().get();
        Response response = exception.get();
        Assert.assertEquals("Request return wrong response", CallbackResourceBase.TRUE, response.readEntity(String.class));

        Response suspendResponse = suspend.get();
        Assert.assertEquals(suspendResponse.getStatusInfo(), Response.Status.INTERNAL_SERVER_ERROR);
        suspendResponse.close();

        Future<Response> error = client.target(generateURL("/resource/error")).request().async().get();
        assertString(error, RuntimeException.class.getName());
        error = client.target(generateURL("/resource/seconderror")).request().async().get();
        assertString(error, RuntimeException.class.getName());
    }

    /**
     * @tpTestDetails Argument contains exception when sending IO exception
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void argumentContainsExceptionWhenSendingIoExceptionTest() throws Exception {
        invokeClear();
        invokeReset();
        Future<Response> suspend = client.target(generateURL("/resource/suspend")).request().async().get();

        Future<Response> register = client.target(generateURL("/resource/register?stage=0")).request().async().get();
        assertString(register, CallbackResourceBase.FALSE);

        Future<Response> exception = client.target(generateURL("/resource/resumechecked?stage=1")).request().async().get();
        Response response = exception.get();
        Assert.assertEquals("Request return wrong response", CallbackResourceBase.TRUE, response.readEntity(String.class));

        Response suspendResponse = suspend.get();
        Assert.assertEquals(suspendResponse.getStatusInfo(), Response.Status.INTERNAL_SERVER_ERROR);
        suspendResponse.close();

        Future<Response> error = client.target(generateURL("/resource/error")).request().async().get();
        assertString(error, IOException.class.getName());
    }
}
