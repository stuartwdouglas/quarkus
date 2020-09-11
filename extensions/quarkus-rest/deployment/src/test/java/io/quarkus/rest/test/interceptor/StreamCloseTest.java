package io.quarkus.rest.test.interceptor;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.interceptor.resource.InterceptorStreamResource;
import io.quarkus.rest.test.interceptor.resource.TestInterceptor;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.utils.PermissionUtil;
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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.lang.reflect.ReflectPermission;
import java.net.SocketPermission;
import java.util.PropertyPermission;

/**
 * @tpSubChapter Interceptors
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.1.2
 * @tpTestCaseDetails Verify outpustream close is invoked on server side (https://issues.jboss.org/browse/RESTEASY-1650)
 */
@RunWith(Arquillian.class)
public class StreamCloseTest
{
   @Deployment
   public static Archive<?> deploy()
   {
      WebArchive war = TestUtil.prepareArchive(StreamCloseTest.class.getSimpleName());

      return TestUtil.finishContainerPrepare(war, null, InterceptorStreamResource.class, TestInterceptor.class, PortProviderUtil.class);
   }

   static Client client;

   @Before
   public void setup()
   {
      client = ClientBuilder.newClient();
   }

   @After
   public void cleanup()
   {
      client.close();
   }

   private String generateURL(String path)
   {
      return PortProviderUtil.generateURL(path, StreamCloseTest.class.getSimpleName());
   }

   @Test
   public void testPriority() throws Exception
   {
      final int count = TestInterceptor.closeCounter.get();
      Response response = client.target(generateURL("/test")).request().post(Entity.text("test"));
      response.bufferEntity();
      Assert.assertEquals("Wrong response status, interceptors don't work correctly", HttpResponseCodes.SC_OK,
            response.getStatus());
      Assert.assertEquals("Wrong content of response, interceptors don't work correctly", "test",
            response.readEntity(String.class));
      response.close();
      Assert.assertEquals(1, TestInterceptor.closeCounter.get() - count);

   }
}
