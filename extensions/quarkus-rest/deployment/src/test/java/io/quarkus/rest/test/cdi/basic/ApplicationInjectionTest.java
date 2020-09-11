package io.quarkus.rest.test.cdi.basic;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.cdi.basic.resource.ApplicationInjection;
import org.jboss.resteasy.utils.PermissionUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
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

import java.lang.reflect.ReflectPermission;
import java.util.PropertyPermission;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for injecting of Application
 * @tpSince RESTEasy 3.0.16
 */
@RunWith(Arquillian.class)
public class ApplicationInjectionTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      // Arquillian in the deployment
      war.addAsManifestResource(PermissionUtil.createPermissionsXmlAsset(new ReflectPermission("suppressAccessChecks"),
            new RuntimePermission("accessDeclaredMembers"),
               new PropertyPermission("arquillian.*", "read")), "permissions.xml");
      war.addClass(ApplicationInjection.class);
      return war;
   }});

   /**
    * @tpTestDetails Injected application instance should not be null.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testAppInjection() throws Exception {
      Assert.assertEquals("Wrong count of initialized applications", 1, ApplicationInjection.instances.size());
      ApplicationInjection app = ApplicationInjection.instances.iterator().next();
      Assert.assertNotNull("Injected application instance should not be null", app.app);
   }
}
