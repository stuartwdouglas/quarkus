package io.quarkus.rest.test.client.proxy;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.client.proxy.resource.WhiteSpaceResource;
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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpSince RESTEasy 3.0.16
 */
public class WhitespaceTest {

   static QuarkusRestClient client;
   private static final String SPACES_REQUEST = "something something";

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(WhitespaceTest.class);
      return TestUtil.finishContainerPrepare(war, null, WhiteSpaceResource.class);
   }});

   @Before
   public void init() {
      client = (QuarkusRestClient)ClientBuilder.newClient();
   }

   @After
   public void after() throws Exception {
      client.close();
   }

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, WhitespaceTest.class.getSimpleName());
   }

   @Path(value = "/sayhello")
   public interface HelloClient {

      @GET
      @Path("/en/{in}")
      @Produces("text/plain")
      String sayHi(@PathParam(value = "in") String in);
   }

   /**
    * @tpTestDetails Client sends GET requests thru client proxy. The string parameter passed in the request has white
    * space in it. The parameter is delivered with white space in the response as well.
    * @tpPassCrit The response entity contains white space as the original request
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testEchoWithWhiteSpace() {
      HelloClient proxy = client.target(generateURL("")).proxy(HelloClient.class);
      Assert.assertEquals(SPACES_REQUEST, proxy.sayHi(SPACES_REQUEST));
   }
}
