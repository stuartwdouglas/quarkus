package io.quarkus.rest.test.exception;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.microprofile.client.BuilderResolver;
import io.quarkus.rest.test.exception.resource.ExceptionMapperRuntimeExceptionWithReasonMapper;
import io.quarkus.rest.test.exception.resource.ResponseExceptionMapperRuntimeExceptionMapper;
import io.quarkus.rest.test.exception.resource.ResponseExceptionMapperRuntimeExceptionResource;
import io.quarkus.rest.test.exception.resource.ResponseExceptionMapperRuntimeExceptionResourceInterface;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import io.quarkus.rest.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.rest.test.simple.TestUtil;

import javax.ws.rs.WebApplicationException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URL;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.6.0
 * @tpTestCaseDetails Regression test for RESTEASY-1847
 */
public class ResponseExceptionMapperRuntimeExceptionTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, ResponseExceptionMapperRuntimeExceptionMapper.class, ExceptionMapperRuntimeExceptionWithReasonMapper.class,
               ResponseExceptionMapperRuntimeExceptionResource.class, ResponseExceptionMapperRuntimeExceptionResourceInterface.class, ResponseExceptionMapper.class);
   }});

   /**
    * @tpTestDetails Check ExceptionMapper for WebApplicationException
    * @tpSince RESTEasy 3.6.0
    */
   @Test
   public void testRuntimeApplicationException() throws Exception {
      ResponseExceptionMapperRuntimeExceptionResourceInterface service = BuilderResolver.instance()
            .newBuilder()
            .baseUrl(new URL(PortProviderUtil.generateURL("/test",
                  ResponseExceptionMapperRuntimeExceptionTest.class.getSimpleName())))
            .register(ResponseExceptionMapperRuntimeExceptionMapper.class)
            .build(ResponseExceptionMapperRuntimeExceptionResourceInterface.class);
      try {
         service.get();
         fail("Should not get here");
      } catch (RuntimeException e) {
         // assert test exception message
         assertEquals(ExceptionMapperRuntimeExceptionWithReasonMapper.REASON, e.getMessage());
      }
   }

   @Test
   public void testRuntimeAException() throws Exception {
      ResponseExceptionMapperRuntimeExceptionResourceInterface service = BuilderResolver.instance()
              .newBuilder()
              .baseUrl(new URL(PortProviderUtil.generateURL("/test",
                      ResponseExceptionMapperRuntimeExceptionTest.class.getSimpleName())))
              .build(ResponseExceptionMapperRuntimeExceptionResourceInterface.class);
      try {
         service.get();
         fail("Should not get here");
      } catch (WebApplicationException e) {
         String str = e.getResponse().readEntity(String.class);
         Assert.assertEquals("Test error occurred", str);
      }
   }


}
