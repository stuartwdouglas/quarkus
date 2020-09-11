package io.quarkus.rest.test.exception;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.exception.resource.ExceptionHandlingProvider;
import io.quarkus.rest.test.exception.resource.ExceptionHandlingResource;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.Assert;
import io.quarkus.rest.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.rest.test.simple.TestUtil;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpSince RESTEasy 3.0.16
 */
public class ExceptionHandlingTest {

   static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(ExceptionHandlingTest.class);
      return TestUtil.finishContainerPrepare(war, null, ExceptionHandlingResource.class, ExceptionHandlingProvider.class);
   }});

   @Before
   public void init() {
      client = (QuarkusRestClient)ClientBuilder.newClient();
   }

   @After
   public void after() throws Exception {
      client.close();
      client = null;
   }

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, ExceptionHandlingTest.class.getSimpleName());
   }

   @Path("/")
   public interface ThrowsExceptionInterface {
      @Path("test")
      @POST
      void post() throws Exception;
   }

   /**
    * @tpTestDetails POST request is sent by client via client proxy. The resource on the server throws exception,
    * which is handled by ExceptionMapper.
    * @tpPassCrit The response with expected Exception text is returned
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testThrowsException() throws Exception {

      ThrowsExceptionInterface proxy = client.target(generateURL("/")).proxy(ThrowsExceptionInterface.class);
      try {
         proxy.post();
      } catch (InternalServerErrorException e) {
         Response response = e.getResponse();
         String errorText = response.readEntity(String.class);
         Assert.assertNotNull("Missing the expected exception text", errorText);
      }

   }

}
