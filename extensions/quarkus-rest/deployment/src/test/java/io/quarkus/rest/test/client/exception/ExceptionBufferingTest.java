package io.quarkus.rest.test.client.exception;

import static org.junit.Assert.fail;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import io.quarkus.rest.test.client.exception.resource.ExceptionBufferingResource;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import io.quarkus.rest.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.rest.test.simple.TestUtil;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpSince RESTEasy 3.0.16
 * @tpTestCaseDetails Regression test for RESTEASY-981
 */
public class ExceptionBufferingTest {

   protected static final Logger logger = LogManager.getLogger(ExceptionBufferingTest.class.getName());

   private static final String DEPLOYMENT_TRUE = "buffer";
   private static final String DEPLOYMENT_FALSE = "nobuffer";
   private static final String DEPLOYMENT_DEFAULT = "default";

   protected static QuarkusRestClient client;


    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      Map<String, String> params = new HashMap<>();
      params.put("resteasy.buffer.exception.entity", "true");
      return TestUtil.finishContainerPrepare(war, params, ExceptionBufferingResource.class);
   }});

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      Map<String, String> params = new HashMap<>();
      params.put("resteasy.buffer.exception.entity", "false");
      return TestUtil.finishContainerPrepare(war, params, ExceptionBufferingResource.class);
   }});

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      client = (QuarkusRestClient)ClientBuilder.newClient();
      return TestUtil.finishContainerPrepare(war, null, ExceptionBufferingResource.class);
   }});

   @AfterClass
   public static void init() throws Exception {
      client.close();
   }


   /**
    * @tpTestDetails Test default value of resteasy.buffer.exception.entity property
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testBufferedResponseDefault() throws Exception {
      Response response = null;

      try {
         ResteasyWebTarget target = client.target(PortProviderUtil.generateURL("/test", DEPLOYMENT_DEFAULT));
         Invocation invocation = target.request().buildGet();
         response = invocation.invoke();
         logger.info("status: " + response.getStatus());
         String s = ClientInvocation.extractResult(new GenericType<String>(String.class), response, null);
         fail("Was expecting an exception: " + s);
      } catch (Exception e) {
         logger.info("caught: " + e);
         String entity = response.readEntity(String.class);
         logger.info("exception entity: " + entity);
         Assert.assertEquals("Wrong response content", "test", entity);
      }
   }

   /**
    * @tpTestDetails Test false value of resteasy.buffer.exception.entity property
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testBufferedResponseFalse() throws Exception {
      Response response = null;

      try {
         ResteasyWebTarget target = client.target(PortProviderUtil.generateURL("/test", DEPLOYMENT_FALSE));
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
            Assert.assertTrue("Wrong exception thrown", e1.getCause() instanceof IOException);
            Assert.assertEquals("Attempted read on closed stream.", e1.getCause().getMessage());
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
   public void testBufferedResponseTrue() throws Exception {
      Response response = null;

      try {
         ResteasyWebTarget target = client.target(PortProviderUtil.generateURL("/test", DEPLOYMENT_TRUE));
         Invocation invocation = target.request().buildGet();
         response = invocation.invoke();
         logger.info("status: " + response.getStatus());
         String s = ClientInvocation.extractResult(new GenericType<String>(String.class), response, null);
         fail("Was expecting an exception: " + s);
      } catch (Exception e) {
         logger.info("caught: " + e);
         String entity = response.readEntity(String.class);
         logger.info("exception entity: " + entity);
         Assert.assertEquals("Wrong responce content", "test", entity);
      }
   }
}
