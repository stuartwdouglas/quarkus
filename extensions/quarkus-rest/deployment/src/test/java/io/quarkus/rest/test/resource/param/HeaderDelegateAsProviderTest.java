package io.quarkus.rest.test.resource.param;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.resource.param.resource.HeaderDelegateAsProviderHeader;
import io.quarkus.rest.test.resource.param.resource.HeaderDelegateAsProviderHeaderDelegate;
import io.quarkus.rest.test.resource.param.resource.HeaderDelegateAsProviderResource;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import io.quarkus.rest.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.rest.test.simple.TestUtil;

/**
 * @tpSubChapter HeaderDelegates discovered via @Provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-2059
 * @tpSince RESTEasy 4.0.0
 */
public class HeaderDelegateAsProviderTest {

   private static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(HeaderDelegateAsProviderHeader.class);
      war.addClass(HeaderDelegateAsProviderHeaderDelegate.class);
      war.addAsResource(HeaderDelegateAsProviderTest.class.getPackage(),
         "javax.ws.rs.ext.Providers_HeaderDelegateAsProvider",
         "META-INF/services/javax.ws.rs.ext.Providers");
      return TestUtil.finishContainerPrepare(war, null, HeaderDelegateAsProviderResource.class);
   }});

   @BeforeClass
   public static void init() {
      client = ClientBuilder.newClient();
      client.register(HeaderDelegateAsProviderHeaderDelegate.class);
   }

   @AfterClass
   public static void after() throws Exception {
      client.close();
   }

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, HeaderDelegateAsProviderTest.class.getSimpleName());
   }

   /**
   * @tpTestDetails Verify HeaderDelegate is discovered and used sending header from server
   * @tpSince RESTEasy 4.0.0
   */
   @Test
   public void testHeaderDelegateServer() {
      Response response = client.target(generateURL("/server")).request().get();
      Assert.assertEquals("toString:abc;xyz", response.getHeaderString("HeaderTest"));
   }

   /**
   * @tpTestDetails Verify HeaderDelegate is discovered and used sending header from client, injected as @HeaderParam
   * @tpSince RESTEasy 4.0.0
   */
   @Test
   public void testHeaderDelegateClientHeader() {
      Builder request = client.target(generateURL("/client/header")).request();
      String response = request.header("HeaderTest", new HeaderDelegateAsProviderHeader("123", "789")).get(String.class);
      Assert.assertEquals("fromString:toString:123|789", response);
   }

   /**
   * @tpTestDetails Verify HeaderDelegate is discovered and used sending header from client, injected as @Context HttpHeaders
   * @tpSince RESTEasy 4.0.0
   */
   @Test
   public void testHeaderDelegateClientHeaders() {
      Builder request = client.target(generateURL("/client/headers")).request();
      String response = request.header("HeaderTest", new HeaderDelegateAsProviderHeader("123", "789")).get(String.class);
      Assert.assertEquals("toString:123;789", response);
   }
}
