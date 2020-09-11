package io.quarkus.rest.test.core.basic;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.core.basic.resource.AnnotationInheritanceNotAResource;
import io.quarkus.rest.test.core.basic.resource.AnnotationInheritanceSomeOtherInterface;
import io.quarkus.rest.test.core.basic.resource.AnnotationInheritanceSomeOtherResource;
import io.quarkus.rest.test.core.basic.resource.AnnotationInheritanceSuperInt;
import io.quarkus.rest.test.core.basic.resource.AnnotationInheritanceSuperIntAbstract;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
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
 * @tpSubChapter Configuration
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for resource without @Path annotation.
 * @tpSince RESTEasy 3.0.16
 */
public class AnnotationInheritanceTest {
   static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClasses(AnnotationInheritanceSuperInt.class, AnnotationInheritanceSuperIntAbstract.class,
            AnnotationInheritanceNotAResource.class, AnnotationInheritanceSomeOtherInterface.class);
      return TestUtil.finishContainerPrepare(war, null, AnnotationInheritanceSomeOtherResource.class);
   }});

   @Before
   public void init() {
      client = (QuarkusRestClient)ClientBuilder.newClient();
   }

   @After
   public void after() throws Exception {
      client.close();
   }

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, AnnotationInheritanceTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Test basic functionality of test resource
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testSuperclassInterfaceAnnotation() {
      AnnotationInheritanceSomeOtherInterface proxy = client.target(generateURL("/somewhere")).proxy(AnnotationInheritanceSomeOtherInterface.class);
      Assert.assertEquals("Foo: Fred", proxy.getSuperInt().getFoo());
   }

   /**
    * @tpTestDetails Check wrong resource without @Path annotation
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testDetectionOfNonResource() {
      try {
         AnnotationInheritanceSomeOtherInterface proxy = client.target(generateURL("/somewhere")).proxy(AnnotationInheritanceSomeOtherInterface.class);
         proxy.getFailure().blah();
         Assert.fail();
      } catch (Exception e) {
         // exception thrown
      }
   }
}
