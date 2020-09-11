package io.quarkus.rest.test.client;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.client.resource.InputStreamResource;
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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.io.InputStream;


/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpSince RESTEasy 3.0.16
 */
public class InputStreamTest extends ClientTestBase{

   @Path("/")
   public interface InputStreamInterface {
      @Path("test")
      @Produces("text/plain")
      @GET
      default InputStream get() {
         return null;
      }

   }

   protected static final Logger logger = LogManager.getLogger(InputStreamTest.class.getName());

   static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(InputStreamTest.class);
      return TestUtil.finishContainerPrepare(war, null, InputStreamResource.class);
   }});

   @Before
   public void init() {
      client = (QuarkusRestClient)ClientBuilder.newClient();
   }

   @After
   public void after() throws Exception {
      client.close();
   }


   /**
    * @tpTestDetails Client sends GET request with requested return type of InputStream.
    * @tpPassCrit The response String can be read from returned input stream and matches expected text.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testInputStream() throws Exception {
      InputStream is = client.target(generateURL("/test")).request().get(InputStream.class);
      byte[] buf = IOUtils.toByteArray(is);
      String str = new String(buf);
      Assert.assertEquals("The returned inputStream doesn't contain expexted text", "hello world", str);
      logger.info("Text from inputstream: " + str);
      is.close();
   }

   /**
    * @tpTestDetails Client sends GET request with requested return type of InputStream. The request is created
    * via client proxy
    * @tpPassCrit The response with expected Exception text is returned
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testInputStreamProxy() throws Exception {
      InputStreamInterface proxy = client.target(generateURL("/")).proxy(InputStreamInterface.class);
      InputStream is = proxy.get();
      byte[] buf = IOUtils.toByteArray(is);
      String str = new String(buf);
      Assert.assertEquals("The returned inputStream doesn't contain expexted text", "hello world", str);
      is.close();
   }

}
