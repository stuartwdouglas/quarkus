package io.quarkus.rest.test.client.proxy;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.client.proxy.resource.ProxyInputStreamProxy;
import io.quarkus.rest.test.client.proxy.resource.ProxyInputStreamResource;
import org.jboss.resteasy.util.ReadFromStream;
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

import java.io.InputStream;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-351
 * @tpSince RESTEasy 3.0.16
 */
public class ProxyInputStreamTest {

   static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(ProxyInputStreamProxy.class);
      return TestUtil.finishContainerPrepare(war, null, ProxyInputStreamResource.class);
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, ProxyInputStreamTest.class.getSimpleName());
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
    * @tpTestDetails New client version
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testInputStreamNewClient() throws Exception {
      ProxyInputStreamProxy proxy = client.target(generateURL("/")).proxy(ProxyInputStreamProxy.class);
      InputStream is = proxy.get();
      byte[] bytes = ReadFromStream.readFromStream(100, is);
      is.close();
      String str = new String(bytes);
      Assert.assertEquals("hello world", str);
   }
}
