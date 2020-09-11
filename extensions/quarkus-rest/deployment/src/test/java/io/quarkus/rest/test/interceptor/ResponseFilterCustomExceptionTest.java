package io.quarkus.rest.test.interceptor;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.interceptor.resource.CustomException;
import io.quarkus.rest.test.interceptor.resource.ThrowCustomExceptionResponseFilter;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import io.quarkus.rest.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.rest.test.simple.TestUtil;

/**
 * @tpSubChapter Interceptors
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.21
 * @tpTestCaseDetails Throw custom exception from a ClientResponseFilter [RESTEASY-1591]
 */
public class ResponseFilterCustomExceptionTest {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClasses(TestUtil.class, PortProviderUtil.class);
      war.addClasses(CustomException.class);
      return TestUtil.finishContainerPrepare(war, null, ThrowCustomExceptionResponseFilter.class);
   }});

   static Client client;

   @Before
   public void setup() {
      client = ClientBuilder.newClient();
   }

   @After
   public void cleanup() {
      client.close();
   }

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, ResponseFilterCustomExceptionTest.class.getSimpleName());
   }
   /**
    * @tpTestDetails Use ClientResponseFilter
    * @tpSince RESTEasy 3.0.21
    */
   @Test
   public void testThrowCustomException() throws Exception {
      try {
         client.register(ThrowCustomExceptionResponseFilter.class);
         client.target(generateURL("/testCustomException")).request().post(Entity.text("testCustomException"));
      } catch (ProcessingException pe) {
         Assert.assertEquals(CustomException.class, pe.getCause().getClass());
      }
   }
}
