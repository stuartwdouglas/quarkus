package io.quarkus.rest.test.providers.jackson2;

import static org.hamcrest.CoreMatchers.containsString;

import java.io.File;
import java.lang.reflect.ReflectPermission;
import java.util.PropertyPermission;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.spi.HttpResponseCodes;
import io.quarkus.rest.test.providers.jackson2.resource.CustomJackson2ProviderApplication;
import io.quarkus.rest.test.providers.jackson2.resource.CustomJackson2ProviderResource;
import org.jboss.resteasy.utils.PermissionUtil;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.resteasy.utils.maven.MavenUtil;
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


/**
 * @tpSubChapter Jackson2 provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.23
 */
public class CustomJackson2ProviderTest {

   private static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClasses(CustomJackson2ProviderApplication.class, CustomJackson2ProviderResource.class);
      war.addAsWebInfResource(CustomJackson2ProviderTest.class.getPackage(), "jboss-deployment-structure-exclude-jaxrs.xml","jboss-deployment-structure.xml");

      MavenUtil mavenUtil;
      mavenUtil = MavenUtil.create(true);
      String version = System.getProperty("project.version");
      try {
         war.addAsLibraries(mavenUtil.createMavenGavRecursiveFiles("org.jboss.resteasy:resteasy-servlet-initializer:" + version).toArray(new File[]{}});));
         war.addAsLibraries(mavenUtil.createMavenGavRecursiveFiles("org.jboss.resteasy:resteasy-core:" + version).toArray(new File[]{}));
         war.addAsLibraries(mavenUtil.createMavenGavRecursiveFiles("org.jboss.resteasy:resteasy-core-spi:" + version).toArray(new File[]{}));
         war.addAsLibraries(mavenUtil.createMavenGavRecursiveFiles("org.jboss.resteasy:resteasy-jackson2-provider:" + version).toArray(new File[]{}));

      } catch (Exception e) {
         throw new RuntimeException("Unable to get artifacts from maven via Aether library", e);
      }
      return war;
   }

   @Before
   public void init() {
      client = ClientBuilder.newClient();
   }

   @After
   public void after() throws Exception {
      client.close();
   }

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, CustomJackson2ProviderTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Deployment contains jboss-deployment-structure.xml, which excludes jaxrs subsystem and brings its
    * own version of resteasy libraries (resteasy-servlet-initializer, resteasy-jaxrs, resteasy-jackson2-provider and it's dependencies).
    * Test verifies that Jackson2Provider from .war archive was loaded instead of the container default one.
    * @tpPassCrit The resource returns Success response
    * @tpSince RESTEasy 3.0.23
    */
   @Test
   public void testCustomUsed() {
      WebTarget target = client.target(generateURL("/jackson2providerpath"));
      Response response = target.request().get();
      String entity = response.readEntity(String.class);
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      Assert.assertThat("Jackson2Provider jar was loaded from unexpected source",
            entity, containsString(CustomJackson2ProviderTest.class.getSimpleName() + ".war/WEB-INF/lib/resteasy-jackson2-provider"));
   }


}
