package io.quarkus.rest.test.providers.custom;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.providers.custom.resource.WriterNotBuiltinTestWriter;
import io.quarkus.rest.test.providers.custom.resource.ReaderWriterCustomer;
import io.quarkus.rest.test.providers.custom.resource.ReaderWriterResource;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.utils.PermissionUtil;
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

import javax.ws.rs.core.Response;
import java.lang.reflect.ReflectPermission;
import java.net.SocketPermission;
import java.security.SecurityPermission;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyPermission;
import java.util.logging.LoggingPermission;

/**
 * @tpSubChapter Providers
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1
 * @tpSince RESTEasy 3.0.16
 */
@RunWith(Arquillian.class)
public class WriterNotBuiltinTest {

   static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(ReaderWriterCustomer.class);
      war.addClass(PortProviderUtil.class);

      Map<String, String> contextParams = new HashMap<>();
      contextParams.put("resteasy.use.builtin.providers", "false");
      // Arquillian in the deployment

      return TestUtil.finishContainerPrepare(war, contextParams, WriterNotBuiltinTestWriter.class, ReaderWriterResource.class);
   }});

   /**
    * @tpTestDetails A more complete test for RESTEASY-1.
    * TestReaderWriter has no type parameter,
    * so it comes after DefaultPlainText in the built-in ordering.
    * The fact that TestReaderWriter gets called verifies that
    * DefaultPlainText gets passed over.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void test1New() throws Exception {
      client = (QuarkusRestClient)ClientBuilder.newClient();
      Response response = client.target(PortProviderUtil.generateURL("/string", WriterNotBuiltinTest.class.getSimpleName()))
            .request().get();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      Assert.assertEquals("text/plain;charset=UTF-8", response.getStringHeaders().getFirst("content-type"));
      Assert.assertEquals("Response contains wrong content", "hello world", response.readEntity(String.class));
      Assert.assertTrue("Wrong MessageBodyWriter was used", WriterNotBuiltinTestWriter.used);
      client.close();
   }
}
