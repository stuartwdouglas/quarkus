package io.quarkus.rest.test.resource.path;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.resource.path.resource.LocatorWithClassHierarchyLocatorResource;
import io.quarkus.rest.test.resource.path.resource.LocatorWithClassHierarchyMiddleResource;
import io.quarkus.rest.test.resource.path.resource.LocatorWithClassHierarchyParamEntityPrototype;
import io.quarkus.rest.test.resource.path.resource.LocatorWithClassHierarchyParamEntityWithConstructor;
import io.quarkus.rest.test.resource.path.resource.LocatorWithClassHierarchyPathParamResource;
import io.quarkus.rest.test.resource.path.resource.LocatorWithClassHierarchyPathSegmentImpl;
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
import javax.ws.rs.core.Response;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class LocatorWithClassHierarchyTest {

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

      war.addClasses(LocatorWithClassHierarchyPathSegmentImpl.class, LocatorWithClassHierarchyMiddleResource.class,
            LocatorWithClassHierarchyPathParamResource.class, LocatorWithClassHierarchyParamEntityWithConstructor.class,
            LocatorWithClassHierarchyParamEntityPrototype.class);
      return TestUtil.finishContainerPrepare(war, null, LocatorWithClassHierarchyLocatorResource.class);
   }});

   /**
    * @tpTestDetails Client sends POST request with null entity for the resource Locator, which creates the targeted
    * resource object.
    * @tpPassCrit Correct response is returned from the server
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testLocatorWithSubWithPathAnnotation() {
      Response response = client.target(PortProviderUtil.generateURL("/resource/locator/ParamEntityWithConstructor/ParamEntityWithConstructor=JAXRS", LocatorWithClassHierarchyTest.class.getSimpleName())).request().post(null);
      Assert.assertEquals(200, response.getStatus());
      response.close();
   }
}
