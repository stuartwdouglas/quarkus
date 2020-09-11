package io.quarkus.rest.test.providers.custom;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.ContainerConstants;
import io.quarkus.rest.test.providers.custom.resource.DuplicateProviderRegistrationFeature;
import io.quarkus.rest.test.providers.custom.resource.DuplicateProviderRegistrationFilter;
import io.quarkus.rest.test.providers.custom.resource.DuplicateProviderRegistrationInterceptor;
import org.jboss.resteasy.utils.PermissionUtil;
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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Feature;
import javax.ws.rs.ext.ReaderInterceptor;
import java.io.File;
import java.io.FilePermission;
import java.lang.reflect.ReflectPermission;
import java.util.PropertyPermission;
import java.util.logging.LoggingPermission;

import static io.quarkus.rest.test.ContainerConstants.DEFAULT_CONTAINER_QUALIFIER;

/**
 * @tpSubChapter Providers
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for JBEAP-4703
 * @tpSince RESTEasy 3.0.17
 */
@RunWith(Arquillian.class)
public class DuplicateProviderRegistrationTest {

   private static final String RESTEASY_002155_ERR_MSG = "Wrong count of RESTEASY002155 warning message";
   private static final String RESTEASY_002160_ERR_MSG = "Wrong count of RESTEASY002160 warning message";

   @SuppressWarnings(value = "unchecked")
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClasses(DuplicateProviderRegistrationFeature.class, DuplicateProviderRegistrationFilter.class,
            TestUtil.class, DuplicateProviderRegistrationInterceptor.class, ContainerConstants.class);
      // Arquillian in the deployment, test reads the server.log
      war.addAsManifestResource(PermissionUtil.createPermissionsXmlAsset(
            new ReflectPermission("suppressAccessChecks"),
            new FilePermission(TestUtil.getStandaloneDir(DEFAULT_CONTAINER_QUALIFIER) + File.separator + "log" +
                  File.separator + "server.log", "read"),
            new LoggingPermission("control", ""),
            new PropertyPermission("arquillian.*", "read"),
            new PropertyPermission("jboss.home.dir", "read"),
              new PropertyPermission("jboss.server.base.dir", "read"),
            new RuntimePermission("accessDeclaredMembers")
      ), "permissions.xml");
      return TestUtil.finishContainerPrepare(war, null, (Class<?>[]) null);
   }});

   private static int getRESTEASY002155WarningCount() {
      return TestUtil.getWarningCount("RESTEASY002155", true, DEFAULT_CONTAINER_QUALIFIER);
   }

   private static int getRESTEASY002160WarningCount() {
      return TestUtil.getWarningCount("RESTEASY002160", true, DEFAULT_CONTAINER_QUALIFIER);
   }

   /**
    * @tpTestDetails Basic test
    * @tpSince RESTEasy 3.0.17
    */
   @Test
   public void testDuplicateProvider() {
      int initRESTEASY002160WarningCount = getRESTEASY002160WarningCount();
      Client client = ClientBuilder.newClient();
      try {
         WebTarget webTarget = client.target("http://www.changeit.com");
         // DuplicateProviderRegistrationFeature will be registered third on the same webTarget even if
         //   webTarget.getConfiguration().isRegistered(DuplicateProviderRegistrationFeature.class)==true
         webTarget.register(DuplicateProviderRegistrationFeature.class).register(new DuplicateProviderRegistrationFeature()).register(new DuplicateProviderRegistrationFeature());
      } finally {
         client.close();
      }
      Assert.assertEquals(RESTEASY_002160_ERR_MSG, 2, getRESTEASY002160WarningCount() - initRESTEASY002160WarningCount);
   }

   /**
    * @tpTestDetails This test is taken from javax.ws.rs.core.Configurable javadoc
    * @tpSince RESTEasy 3.0.17
    */
   @Test
   public void testFromJavadoc() {
      int initRESTEASY002155WarningCount = getRESTEASY002155WarningCount();
      int initRESTEASY002160WarningCount = getRESTEASY002160WarningCount();
      Client client = ClientBuilder.newClient();
      try {
         WebTarget webTarget = client.target("http://www.changeit.com");
         webTarget.register(DuplicateProviderRegistrationInterceptor.class, ReaderInterceptor.class);
         webTarget.register(DuplicateProviderRegistrationInterceptor.class);       // Rejected by runtime.
         webTarget.register(new DuplicateProviderRegistrationInterceptor());       // Rejected by runtime.
         webTarget.register(DuplicateProviderRegistrationInterceptor.class, 6500); // Rejected by runtime.

         webTarget.register(new DuplicateProviderRegistrationFeature());
         webTarget.register(new DuplicateProviderRegistrationFeature()); // rejected by runtime.
         webTarget.register(DuplicateProviderRegistrationFeature.class);   // rejected by runtime.
         webTarget.register(DuplicateProviderRegistrationFeature.class, Feature.class);  // Rejected by runtime.
      } finally {
         client.close();
      }
      Assert.assertEquals(RESTEASY_002155_ERR_MSG, 4, getRESTEASY002155WarningCount() - initRESTEASY002155WarningCount);
      Assert.assertEquals(RESTEASY_002160_ERR_MSG, 2, getRESTEASY002160WarningCount() - initRESTEASY002160WarningCount);
   }
}
