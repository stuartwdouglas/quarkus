package io.quarkus.rest.test.microprofile.restclient;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.microprofile.restclient.resource.RestClientProxyRedeployRemoteService;
import io.quarkus.rest.test.microprofile.restclient.resource.RestClientProxyRedeployResource;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
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

public class RestClientProxyRedeployTest
{
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(RestClientProxyRedeployRemoteService.class);
      war.addAsManifestResource(new StringAsset("Dependencies: org.eclipse.microprofile.restclient"), "MANIFEST.MF");
      war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
      return TestUtil.finishContainerPrepare(war, null, RestClientProxyRedeployResource.class);
   }});

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(RestClientProxyRedeployRemoteService.class);
      war.addAsManifestResource(new StringAsset("Dependencies: org.eclipse.microprofile.restclient"), "MANIFEST.MF");
      war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
      return TestUtil.finishContainerPrepare(war, null, RestClientProxyRedeployResource.class);
   }});

   private String generateURL(String path, String suffix) {
      return PortProviderUtil.generateURL(path, RestClientProxyRedeployTest.class.getSimpleName() + suffix);
   }

   @Test
   public void testGet1() throws Exception {
      Client client = ClientBuilder.newClient();
      Response response = client.target(generateURL("/test/1", "1")).request().get();
      Assert.assertEquals(200, response.getStatus());
      String entity = response.readEntity(String.class);
      Assert.assertEquals("OK", entity);
      client.close();
   }

   @Test
   public void testGet2() throws Exception {
      Client client = ClientBuilder.newClient();
      Response response = client.target(generateURL("/test/1", "2")).request().get();
      Assert.assertEquals(200, response.getStatus());
      String entity = response.readEntity(String.class);
      Assert.assertEquals("OK", entity);
      client.close();
   }
}