package io.quarkus.rest.test.core.basic;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.core.basic.resource.InternalDispatcherForwardingResource;
import io.quarkus.rest.test.core.basic.resource.InternalDispatcherClient;
import org.jboss.resteasy.utils.PermissionUtil;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestApplication;
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

import javax.ws.rs.core.Response;

import java.lang.reflect.ReflectPermission;
import java.net.SocketPermission;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.PropertyPermission;
import java.util.logging.LoggingPermission;

/**
 * @tpSubChapter Core
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 * @tpTestCaseDetails Test for InternalDispatcher
 */
@RunWith(Arquillian.class)
public class InternalDispatcherTest {
   private static Logger logger = Logger.getLogger(InternalDispatcherTest.class);

   private static InternalDispatcherForwardingResource forwardingResource;
   private static QuarkusRestClient client;
   public static final String PATH = "/foo/bar";

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClasses(InternalDispatcherClient.class, InternalDispatcherForwardingResource.class);
      war.addClasses(TestUtil.class, PortProviderUtil.class);

      List<Class<?>> singletons = new ArrayList<>();
      singletons.add(InternalDispatcherForwardingResource.class);
      // Arquillian in the deployment

      return TestUtil.finishContainerPrepare(war, null, singletons, (Class<?>[]) null);
   }});

   private String generateBaseUrl() {
      return PortProviderUtil.generateBaseUrl(InternalDispatcherTest.class.getSimpleName());
   }

   @Before
   public void setup() {
      client = (QuarkusRestClient)ClientBuilder.newClient();

      Assert.assertTrue("No singleton founded", TestApplication.singletons.iterator().hasNext());
      Object objectResource = TestApplication.singletons.iterator().next();
      Assert.assertTrue("Wrong type of singleton founded", objectResource instanceof InternalDispatcherForwardingResource);
      forwardingResource = (InternalDispatcherForwardingResource) objectResource;
      forwardingResource.uriStack.clear();
   }

   @After
   public void after() throws Exception {
      client.close();
   }

   /**
    * @tpTestDetails Check response of forwarded reuests.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testClientResponse() throws Exception {
      InternalDispatcherClient proxy = client.target(generateBaseUrl()).proxy(InternalDispatcherClient.class);

      Assert.assertEquals("Wrong response", "basic", proxy.getBasic());
      Assert.assertEquals("Wrong response", "basic", proxy.getForwardBasic());
      Assert.assertEquals("Wrong response", "object1", proxy.getObject(1).readEntity(String.class));
      Assert.assertEquals("Wrong response", "object1", proxy.getForwardedObject(1).readEntity(String.class));
      Response cr = proxy.getObject(0);
      Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), cr.getStatus());
      cr.close();
      cr = proxy.getForwardedObject(0);
      Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), cr.getStatus());
      cr.close();

      proxy.putForwardBasic("testBasic");
      Assert.assertEquals("Wrong response", "testBasic", proxy.getBasic());
      proxy.postForwardBasic("testBasic1");
      Assert.assertEquals("Wrong response", "testBasic1", proxy.getBasic());
      proxy.deleteForwardBasic();
      Assert.assertEquals("Wrong response", "basic", proxy.getBasic());

   }

   /**
    * @tpTestDetails assert that even though there were infinite forwards, there still was
    *                only 1 level of "context" data and that clean up occurred correctly.
    *                This should not spin forever, since RESTEasy stops the recursive loop
    *                after 20 internal dispatches
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testInfinitForward() {
      InternalDispatcherClient proxy = client.target(generateBaseUrl()).proxy(InternalDispatcherClient.class);
      Assert.assertEquals("Cleanup was not correctly performed", 1, proxy.infiniteForward());
   }

   /**
    * @tpTestDetails Check UriInfo information without forwarding
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testUriInfoBasic() {
      InternalDispatcherClient proxy = client.target(generateBaseUrl()).proxy(InternalDispatcherClient.class);
      proxy.getBasic();
      Assert.assertEquals("Wrong UriInfo information without forwarding", generateBaseUrl() + "/basic", forwardingResource.uriStack.pop());
      Assert.assertTrue("Wrong UriInfo information without forwarding", forwardingResource.uriStack.isEmpty());

   }

   /**
    * @tpTestDetails Check UriInfo information with forwarding. This is also regression test for JBEAP-2476
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testUriInfoForwardBasic() throws Exception {
      InternalDispatcherClient proxy = client.target(generateBaseUrl()).proxy(InternalDispatcherClient.class);

      logger.info("return value: " + proxy.getForwardBasic());
      int i = 0;
      for (Iterator<String> it = forwardingResource.uriStack.iterator(); it.hasNext(); i++) {
         logger.info(String.format("%d. item in uriStack: %s", i, it.next()));
      }

      Assert.assertEquals("Wrong first URI in stack", generateBaseUrl() + "/basic", forwardingResource.uriStack.pop());
      Assert.assertEquals("Wrong second URI in stack", generateBaseUrl() + "/forward/basic", forwardingResource.uriStack.pop());
      Assert.assertTrue("Only two uri should be in stack", forwardingResource.uriStack.isEmpty());
   }

   /**
    * @tpTestDetails Regression test for JBEAP-2476
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testUriInfoForwardBasicComplexUri() {
      String baseUrl = generateBaseUrl();
      InternalDispatcherClient proxy = client.target(generateBaseUrl()).proxy(InternalDispatcherClient.class);

      proxy.getComplexForwardBasic();
      Assert.assertEquals("Wrong first URI in stack", baseUrl + PATH + "/basic", forwardingResource.uriStack.pop());
      Assert.assertEquals("Wrong second URI in stack", baseUrl + PATH + "/forward/basic", forwardingResource.uriStack.pop());
      Assert.assertTrue("Only two uri should be in stack", forwardingResource.uriStack.isEmpty());
   }
}
