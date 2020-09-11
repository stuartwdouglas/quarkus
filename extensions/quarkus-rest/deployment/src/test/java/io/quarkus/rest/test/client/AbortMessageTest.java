package io.quarkus.rest.test.client;


import java.io.UnsupportedEncodingException;
import java.util.logging.LoggingPermission;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.client.resource.AbortMessageResourceFilter;
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

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpTestCaseDetails RESTEASY-1540
 * @tpSince RESTEasy 3.1.0.Final
 */
public class AbortMessageTest {
   static Client client;

   @BeforeClass
   public static void setup() {
      client = ClientBuilder.newClient();
   }

   @AfterClass
   public static void close() {
      client.close();
   }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);


      return TestUtil.finishContainerPrepare(war, null, AbortMessageResourceFilter.class);
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, AbortMessageTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Send response with "Aborted"
    * @tpSince RESTEasy 3.1.0.Final
    */
   @Test
   public void testAbort() throws UnsupportedEncodingException {
      WebTarget target = client.target(generateURL("/showproblem"));
      Response response = target.request().get();
      Assert.assertEquals(200, response.getStatus());
      Assert.assertEquals("aborted", response.readEntity(String.class));
   }
}
