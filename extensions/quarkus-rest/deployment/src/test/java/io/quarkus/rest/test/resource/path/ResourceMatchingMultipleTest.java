package io.quarkus.rest.test.resource.path;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.resource.path.resource.ResourceMatchingMultipleUserCertResource;
import io.quarkus.rest.test.resource.path.resource.ResourceMatchingMultipleUserMembershipResource;
import io.quarkus.rest.test.resource.path.resource.ResourceMatchingMultipleUserResource;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assert;
import io.quarkus.rest.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.rest.test.simple.TestUtil;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class ResourceMatchingMultipleTest {

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, ResourceMatchingMultipleTest.class.getSimpleName());
   }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, ResourceMatchingMultipleUserResource.class, ResourceMatchingMultipleUserCertResource.class, ResourceMatchingMultipleUserMembershipResource.class);
   }});

   static Client client;

   @BeforeClass
   public static void setup() {
      client = ClientBuilder.newClient();
   }

   @AfterClass
   public static void close() {
      client.close();
   }

   /**
    * @tpTestDetails Client sends GET request for Users resource, with custom id. With 3 Resources available in the
    * application, the correct path will be selected.
    * @tpPassCrit The correct Resource path is chosen
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testMatchingUsers() throws Exception {
      String answer = client.target(generateURL("/users/1")).request().get(String.class);
      Assert.assertEquals("The incorrect resource path was chosen", "users/{id} 1", answer);
   }

   /**
    * @tpTestDetails Client sends GET request for Memberships resource, with custom id. With 3 Resources available in the
    * application, the correct path will be selected.
    * @tpPassCrit The correct Resource path is chosen
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testMatchingMemberShips() throws Exception {
      String answer = client.target(generateURL("/users/1/memberships")).request().get(String.class);
      Assert.assertEquals("The incorrect resource path was chosen", "users/{id}/memberships 1", answer);
   }

   /**
    * @tpTestDetails Client sends GET request for Certs resource, with custom id. With 3 Resources available in the
    * application, the correct path will be selected.
    * @tpPassCrit The correct Resource path is chosen
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testMatchingCerts() throws Exception {
      String answer = client.target(generateURL("/users/1/certs")).request().get(String.class);
      Assert.assertEquals("The incorrect resource path was chosen", "users/{id}/certs 1", answer);
   }


}
