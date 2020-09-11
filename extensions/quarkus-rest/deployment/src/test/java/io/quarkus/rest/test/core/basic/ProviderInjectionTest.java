package io.quarkus.rest.test.core.basic;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.core.basic.resource.ProviderInjectionSimpleMessageBodyWriter;
import io.quarkus.rest.test.core.basic.resource.ProviderInjectionSimpleResource;
import io.quarkus.rest.test.core.basic.resource.ProviderInjectionSimpleResourceImpl;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import io.quarkus.rest.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.rest.test.simple.TestUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @tpSubChapter Providers
 * @tpChapter Integration tests
 * @tpTestCaseDetails This test verifies that Providers instance can be injected into a Provider
 *                    using constructor or field injection.
 * @tpSince RESTEasy 3.0.16
 */
public class ProviderInjectionTest {
   static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(ProviderInjectionSimpleResource.class);
      war.addClass(PortProviderUtil.class);
      return TestUtil.finishContainerPrepare(war, null, ProviderInjectionSimpleMessageBodyWriter.class, ProviderInjectionSimpleResourceImpl.class);
   }});

   @Before
   public void setUp() throws Exception {
      // do a request (force provider instantiation if providers were created lazily)
      client = (QuarkusRestClient)ClientBuilder.newClient();
      ProviderInjectionSimpleResource proxy = client.target(PortProviderUtil.generateBaseUrl(ProviderInjectionTest.class.getSimpleName())).proxyBuilder(ProviderInjectionSimpleResource.class).build();
      assertEquals(proxy.foo(), "bar");
   }

   @After
   public void after() throws Exception {
      client.close();
   }

   /**
    * @tpTestDetails Getting constructor
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testConstructorInjection() {
      for (ProviderInjectionSimpleMessageBodyWriter writer : ProviderInjectionSimpleMessageBodyWriter.getInstances()) {
         assertTrue(writer.getConstructorProviders() != null);
      }
   }

   /**
    * @tpTestDetails Getting field
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testFieldInjection() {
      for (ProviderInjectionSimpleMessageBodyWriter writer : ProviderInjectionSimpleMessageBodyWriter.getInstances()) {
         assertTrue(writer.getFieldProviders() != null);
      }
   }

}
