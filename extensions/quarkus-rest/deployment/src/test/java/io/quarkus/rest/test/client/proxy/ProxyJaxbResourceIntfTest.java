package io.quarkus.rest.test.client.proxy;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.client.jaxrs.ProxyBuilder;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.client.proxy.resource.ProxyJaxbCredit;
import io.quarkus.rest.test.client.proxy.resource.ProxyJaxbResource;
import io.quarkus.rest.test.client.proxy.resource.ProxyJaxbResourceIntf;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;
import io.quarkus.rest.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.rest.test.simple.TestUtil;
import javax.ws.rs.core.Response;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class ProxyJaxbResourceIntfTest {

   static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(ProxyJaxbResourceIntfTest.class);
      war.addClass(ProxyJaxbResourceIntf.class);
      return TestUtil.finishContainerPrepare(war, null, ProxyJaxbResource.class,
            ProxyJaxbCredit.class);
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
      return PortProviderUtil.generateURL(path, ProxyJaxbResourceIntfTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Tests client proxy with annotated jaxb resource, RESTEASY-306
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testIt() throws Exception {
      ProxyJaxbResourceIntf proxy = ProxyBuilder.builder(ProxyJaxbResourceIntf.class, client.target(generateURL("/"))).build();
      Response response = proxy.getCredits("xx");
      Assert.assertEquals(response.getStatus(), HttpResponseCodes.SC_OK);
      ProxyJaxbCredit cred = response.readEntity(ProxyJaxbCredit.class);
      Assert.assertEquals("Unexpected response from the server", "foobar", cred.getName());
   }


}
