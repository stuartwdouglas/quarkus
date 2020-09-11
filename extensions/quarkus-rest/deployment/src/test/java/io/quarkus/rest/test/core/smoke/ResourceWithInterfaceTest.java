package io.quarkus.rest.test.core.smoke;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.core.smoke.resource.ResourceWithInterfaceResourceWithInterface;
import io.quarkus.rest.test.core.smoke.resource.ResourceWithInterfaceSimpleClient;
import org.jboss.resteasy.utils.PortProviderUtil;
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

/**
 * @tpSubChapter Smoke tests for jaxrs
 * @tpChapter Integration tests
 * @tpTestCaseDetails Smoke test for resource with interface.
 * @tpSince RESTEasy 3.0.16
 */
public class ResourceWithInterfaceTest {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(ResourceWithInterfaceSimpleClient.class);
      return TestUtil.finishContainerPrepare(war, null, ResourceWithInterfaceResourceWithInterface.class);
   }});

   /**
    * @tpTestDetails Check result from resource with interface.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testNoDefaultsResource() throws Exception {
      QuarkusRestClient client = (QuarkusRestClient)ClientBuilder.newClient();
      ResourceWithInterfaceSimpleClient proxy = client.target(PortProviderUtil.generateBaseUrl(ResourceWithInterfaceTest.class.getSimpleName())).proxyBuilder(ResourceWithInterfaceSimpleClient.class).build();

      Assert.assertEquals("Wrong client answer.", "basic", proxy.getBasic());
      proxy.putBasic("hello world");
      Assert.assertEquals("Wrong client answer.", "hello world", proxy.getQueryParam("hello world"));
      Assert.assertEquals("Wrong client answer.", 1234, proxy.getUriParam(1234));

      client.close();
   }
}
