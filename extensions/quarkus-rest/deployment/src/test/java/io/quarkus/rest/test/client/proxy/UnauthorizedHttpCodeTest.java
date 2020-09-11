package io.quarkus.rest.test.client.proxy;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.client.proxy.resource.UnauthorizedHttpCodeProxy;
import io.quarkus.rest.test.client.proxy.resource.UnauthorizedHttpCodeResource;
import org.jboss.resteasy.spi.HttpResponseCodes;
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

import javax.ws.rs.NotAuthorizedException;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpSince RESTEasy 3.0.16
 * @tpTestCaseDetails Regression test for RESTEASY-575
 */
public class UnauthorizedHttpCodeTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, UnauthorizedHttpCodeResource.class);
   }});

   /**
    * @tpTestDetails Get 401 http code via proxy
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testProxy() throws Exception {
      QuarkusRestClient client = (QuarkusRestClient)ClientBuilder.newClient();
      UnauthorizedHttpCodeProxy proxy = client.target(PortProviderUtil.generateURL("/", UnauthorizedHttpCodeTest.class.getSimpleName())).proxy(UnauthorizedHttpCodeProxy.class);
      try {
         proxy.getFoo();
      } catch (NotAuthorizedException e) {
         Assert.assertEquals(e.getResponse().getStatus(), HttpResponseCodes.SC_UNAUTHORIZED);
         String val = e.getResponse().readEntity(String.class);
         Assert.assertEquals("Wrong content of response", "hello", val);
      }
      client.close();
   }

}
