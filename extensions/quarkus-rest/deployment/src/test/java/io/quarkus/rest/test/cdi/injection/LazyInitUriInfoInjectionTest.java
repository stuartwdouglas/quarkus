package io.quarkus.rest.test.cdi.injection;

import java.net.URI;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.cdi.injection.resource.LazyInitUriInfoInjectionResource;
import io.quarkus.rest.test.cdi.injection.resource.LazyInitUriInfoInjectionSingletonResource;
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

import javax.ws.rs.client.WebTarget;


/**
 * @tpSubChapter Injection
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-573
 * @tpSince RESTEasy 3.0.16
 */
public class LazyInitUriInfoInjectionTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, LazyInitUriInfoInjectionSingletonResource.class, LazyInitUriInfoInjectionResource.class);
   }});
   @ArquillianResource
   URI baseUri;

   private String generateURL(String path) {
      return baseUri.resolve(path).toString();
   }

   /**
    * @tpTestDetails Repeat client request without query parameter
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testDup() throws Exception {
      QuarkusRestClient client = (QuarkusRestClient)ClientBuilder.newClient();
      WebTarget base = client.target(generateURL("test?h=world"));
      String val = base.request().get().readEntity(String.class);
      Assert.assertEquals(val, "world");

      base = client.target(generateURL("test"));
      val = base.request().get().readEntity(String.class);
      Assert.assertEquals(val, "");
      client.close();
   }
}
