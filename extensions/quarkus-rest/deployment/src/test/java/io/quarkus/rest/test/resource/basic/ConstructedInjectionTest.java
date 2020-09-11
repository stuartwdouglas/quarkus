package io.quarkus.rest.test.resource.basic;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.TestPortProvider;
import io.quarkus.rest.test.resource.basic.resource.ConstructedInjectionResource;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.utils.PermissionUtil;
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
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.PropertyPermission;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class ConstructedInjectionTest {

   static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(TestPortProvider.class);

      // Use of PortProviderUtil in the deployment

      return TestUtil.finishContainerPrepare(war, null, ConstructedInjectionResource.class);
   }});

   @BeforeClass
   public static void init() {
      client = ClientBuilder.newClient();
   }

   @AfterClass
   public static void after() throws Exception {
      client.close();
   }

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, ConstructedInjectionTest.class.getSimpleName());
   }

   private void _test(String path) {
      WebTarget base = client.target(generateURL(path));
      try {
         Response response = base.request().get();
         Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
         response.close();
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * @tpTestDetails Test with the resource containing custom constructor with @Context and @QueryParam injection
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testUriInfo() throws Exception {
      _test("/simple");
   }

}
