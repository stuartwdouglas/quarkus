package io.quarkus.rest.test.client.proxy;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.client.proxy.resource.ClassloaderResource;
import io.quarkus.rest.test.client.proxy.resource.ClientSmokeResource;
import io.quarkus.rest.test.core.smoke.resource.ResourceWithInterfaceSimpleClient;
import org.jboss.resteasy.utils.PermissionUtil;
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

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

public class ProxyClassloaderTest
{

   @Deployment
   public static Archive<?> deploySimpleResource()
   {
      WebArchive war = TestUtil.prepareArchive(ProxyClassloaderTest.class.getSimpleName());
      war.addClass(ResourceWithInterfaceSimpleClient.class);
      war.addAsManifestResource(PermissionUtil.createPermissionsXmlAsset(
              new RuntimePermission("setContextClassLoader"),
              new RuntimePermission("createClassLoader"),
              new RuntimePermission("getClassLoader")
      ), "permissions.xml");
      return TestUtil.finishContainerPrepare(war, null, ClientSmokeResource.class, ClassloaderResource.class);
   }

   @Test
   public void testNoTCCL() throws Exception
   {
      QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
      String target2 = PortProviderUtil.generateURL("", ProxyClassloaderTest.class.getSimpleName());
      String target = PortProviderUtil.generateURL("/cl/cl?param=" + target2, ProxyClassloaderTest.class.getSimpleName());
      Response response = client.target(target).request().get();
      String entity = response.readEntity(String.class);
      Assert.assertEquals("basic", entity);
      response.close();
      client.close();
   }

}
