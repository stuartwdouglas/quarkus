package io.quarkus.rest.test.client.proxy;


import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import io.quarkus.rest.test.client.proxy.resource.NullEntityProxy;
import io.quarkus.rest.test.client.proxy.resource.NullEntityProxyGreeter;
import io.quarkus.rest.test.client.proxy.resource.NullEntityProxyGreeting;
import io.quarkus.rest.test.client.proxy.resource.NullEntityProxyResource;
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

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1684
 * @tpSince RESTEasy 3.0.24
 */
public class NullEntityProxyTest {

   private static QuarkusRestClient client;

   @BeforeClass
   public static void before() throws Exception {
      client = (QuarkusRestClient)ClientBuilder.newClient();
   }

   @AfterClass
   public static void after() throws Exception {
      client.close();
   }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClasses(NullEntityProxy.class, NullEntityProxyGreeting.class, NullEntityProxyGreeter.class);
      return TestUtil.finishContainerPrepare(war, null, NullEntityProxyResource.class);
   }});

   private static String generateBaseUrl() {
      return PortProviderUtil.generateBaseUrl(NullEntityProxyTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Test to send null Entity with proxy
    * @tpSince RESTEasy 3.0.24
    */
   @Test
   public void testNullEntityWithProxy() {
      ResteasyWebTarget target = client.target(generateBaseUrl());
      NullEntityProxy proxy = target.proxy(NullEntityProxy.class);
      NullEntityProxyGreeting greeting = proxy.helloEntity(null);
      Assert.assertEquals("Response has wrong content", null, greeting.getGreeter());
   }
}
