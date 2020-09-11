package io.quarkus.rest.test.client.proxy;

import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import io.quarkus.rest.test.client.proxy.resource.EncodedPathProxyInterface;
import io.quarkus.rest.test.client.proxy.resource.EncodedPathProxyResource;
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
 * @tpTestCaseDetails Regression test for RESTEASY-1475.
 * @tpSince RESTEasy 3.1.4
 */
public class EncodedPathProxyTest {
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

      war.addClasses(EncodedPathProxyInterface.class);
      return TestUtil.finishContainerPrepare(war, null, EncodedPathProxyResource.class);
   }});

   private static String generateBaseUrl() {
      return PortProviderUtil.generateBaseUrl(EncodedPathProxyTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Verify "/" in "t;hawkular/f;jk-feed" is sent encoded
    * @tpSince RESTEasy 3.1.4
    */
   @Test
   public void testEncodeProxy() throws Exception
   {
      ResteasyWebTarget target = client.target(generateBaseUrl());
      EncodedPathProxyInterface proxy = target.proxy(EncodedPathProxyInterface.class);
      Response response = proxy.encode("t;hawkular/f;jk-feed", null);
      Assert.assertEquals(200, response.getStatus());
      String uri = response.readEntity(String.class);
      Assert.assertEquals(generateBaseUrl() + "/test/encode/t;hawkular%2Ff;jk-feed", uri);
   }

   /**
    * @tpTestDetails Verify "/" in "t;hawkular/f;jk-feed" is sent unencoded
    * @tpSince RESTEasy 3.1.4
    */
   @Test
   public void testNoencodeProxy() throws Exception
   {
      ResteasyWebTarget target = client.target(generateBaseUrl());
      EncodedPathProxyInterface proxy = target.proxy(EncodedPathProxyInterface.class);
      Response response = proxy.noencode("t;hawkular/f;jk-feed", null);
      Assert.assertEquals(200, response.getStatus());
      String uri = response.readEntity(String.class);
      Assert.assertEquals(generateBaseUrl() + "/test/noencode/t;hawkular/f;jk-feed", uri);
   }
}
