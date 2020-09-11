package io.quarkus.rest.test.core.smoke;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.core.smoke.resource.ResourceWithMultipleInterfacesEmpty;
import io.quarkus.rest.test.core.smoke.resource.ResourceWithMultipleInterfacesIntA;
import io.quarkus.rest.test.core.smoke.resource.ResourceWithMultipleInterfacesRootResource;
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
 * @tpSubChapter Smoke tests for jaxrs
 * @tpChapter Integration tests
 * @tpTestCaseDetails Smoke test for resource with multiple interfaces.
 * @tpSince RESTEasy 3.0.16
 */
public class ResourceWithMultipleInterfacesTest {

   static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(ResourceWithMultipleInterfacesIntA.class);
      war.addClass(ResourceWithMultipleInterfacesEmpty.class);
      return TestUtil.finishContainerPrepare(war, null, ResourceWithMultipleInterfacesRootResource.class);
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, ResourceWithMultipleInterfacesTest.class.getSimpleName());
   }

   @Before
   public void init() {
      client = (QuarkusRestClient)ClientBuilder.newClient();
   }

   @After
   public void after() throws Exception {
      client.close();
   }

   /**
    * @tpTestDetails Check result from resource with multiple interfaces.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testNoDefaultsResource() throws Exception {
      ResourceWithMultipleInterfacesIntA proxy = client.target(generateURL("/")).proxyBuilder(ResourceWithMultipleInterfacesIntA.class).build();
      Assert.assertEquals("Wrong client answer.", "FOO", proxy.getFoo());
   }
}
