package io.quarkus.rest.test.client.exception;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.client.exception.resource.ExceptionBufferingResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpSince RESTEasy 3.0.16
 * @tpTestCaseDetails Regression test for RESTEASY-981
 */
@DisplayName("Exception Buffering Test")
public class ExceptionBufferingTest {

    protected static final Logger logger = Logger.getLogger(ExceptionBufferingTest.class.getName());

    private static final String DEPLOYMENT_TRUE = "buffer";

    private static final String DEPLOYMENT_FALSE = "nobuffer";

    private static final String DEPLOYMENT_DEFAULT = "default";

    protected static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            Map<String, String> params = new HashMap<>();
            params.put("resteasy.buffer.exception.entity", "true");
            return TestUtil.finishContainerPrepare(war, params, ExceptionBufferingResource.class);
        }
    });

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            Map<String, String> params = new HashMap<>();
            params.put("resteasy.buffer.exception.entity", "false");
            return TestUtil.finishContainerPrepare(war, params, ExceptionBufferingResource.class);
        }
    });

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            client = (QuarkusRestClient) ClientBuilder.newClient();
            return TestUtil.finishContainerPrepare(war, null, ExceptionBufferingResource.class);
        }
    });

    @AfterAll
    public static void init() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Test default value of resteasy.buffer.exception.entity property
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Buffered Response Default")
    public void testBufferedResponseDefault() throws Exception {
        Response response = null;
        try {
            QuarkusRestWebTarget target = client.target(PortProviderUtil.generateURL("/test", DEPLOYMENT_DEFAULT));
            Invocation invocation = target.request().buildGet();
            response = invocation.invoke();
            logger.info("status: " + response.getStatus());
            String s = ClientInvocation.extractResult(new GenericType<String>(String.class), response, null);
            fail("Was expecting an exception: " + s);
        } catch (Exception e) {
            logger.info("caught: " + e);
            String entity = response.readEntity(String.class);
            logger.info("exception entity: " + entity);
            Assertions.assertEquals("test", entity, "Wrong response content");
        }
    }

    /**
     * @tpTestDetails Test false value of resteasy.buffer.exception.entity property
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Buffered Response False")
    public void testBufferedResponseFalse() throws Exception {
        Response response = null;
        try {
            QuarkusRestWebTarget target = client.target(PortProviderUtil.generateURL("/test", DEPLOYMENT_FALSE));
            Invocation invocation = target.request().buildGet();
            response = invocation.invoke();
            logger.info("status: " + response.getStatus());
            String s = ClientInvocation.extractResult(new GenericType<String>(String.class), response, null);
            fail("Was expecting an exception: " + s);
        } catch (Exception e) {
            logger.info("caught: " + e);
            try {
                String s = response.readEntity(String.class);
                fail("Was expecting a second exception: " + s);
            } catch (ProcessingException e1) {
                logger.info("and caught: " + e1);
                Assertions.assertTrue(e1.getCause() instanceof IOException, "Wrong exception thrown");
                Assertions.assertEquals(e1.getCause().getMessage(), "Attempted read on closed stream.");
            } catch (Exception e1) {
                fail("Was expecting a ProcessingException instead of " + e1);
            }
        }
    }

    /**
     * @tpTestDetails Test true value of resteasy.buffer.exception.entity property
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Buffered Response True")
    public void testBufferedResponseTrue() throws Exception {
        Response response = null;
        try {
            QuarkusRestWebTarget target = client.target(PortProviderUtil.generateURL("/test", DEPLOYMENT_TRUE));
            Invocation invocation = target.request().buildGet();
            response = invocation.invoke();
            logger.info("status: " + response.getStatus());
            String s = ClientInvocation.extractResult(new GenericType<String>(String.class), response, null);
            fail("Was expecting an exception: " + s);
        } catch (Exception e) {
            logger.info("caught: " + e);
            String entity = response.readEntity(String.class);
            logger.info("exception entity: " + entity);
            Assertions.assertEquals("test", entity, "Wrong responce content");
        }
    }
}
