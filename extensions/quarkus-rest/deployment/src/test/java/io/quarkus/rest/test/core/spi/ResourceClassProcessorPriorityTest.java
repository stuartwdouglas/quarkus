package io.quarkus.rest.test.core.spi;


import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.core.spi.resource.ResourceClassProcessorPriiorityAImplementation;
import io.quarkus.rest.test.core.spi.resource.ResourceClassProcessorPriiorityBImplementation;
import io.quarkus.rest.test.core.spi.resource.ResourceClassProcessorPriiorityCImplementation;
import io.quarkus.rest.test.core.spi.resource.ResourceClassProcessorPureEndPoint;
import org.jboss.resteasy.spi.HttpResponseCodes;
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

import javax.ws.rs.core.Response;

import java.lang.reflect.ReflectPermission;
import java.net.SocketPermission;
import java.util.ArrayList;
import java.util.List;
import java.util.PropertyPermission;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * @tpSubChapter ResourceClassProcessor SPI
 * @tpChapter Integration tests
 * @tpTestCaseDetails ResourceClassProcessor and Priority annotation test
 * @tpSince RESTEasy 3.6
 */
@RunWith(Arquillian.class)
public class ResourceClassProcessorPriorityTest {

   protected static final Logger logger = Logger.getLogger(ResourceClassProcessorPriorityTest.class.getName());

   private static List<String> visitedProcessors = new ArrayList<>();

   public static synchronized void addToVisitedProcessors(String item) {
      visitedProcessors.add(item);
   }

   static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(ResourceClassProcessorPriorityTest.class);
      war.addClass(PortProviderUtil.class);
      war.addAsManifestResource(PermissionUtil.createPermissionsXmlAsset(
            new SocketPermission(PortProviderUtil.getHost(), "connect,resolve"),
            new PropertyPermission("org.jboss.resteasy.port", "read"),
            new RuntimePermission("getenv.RESTEASY_PORT"),
            new PropertyPermission("ipv6", "read"),
            new PropertyPermission("node", "read"),
            new PropertyPermission("arquillian.*", "read"),
            new RuntimePermission("accessDeclaredMembers"),
            new ReflectPermission("suppressAccessChecks")
      ), "permissions.xml");
      return TestUtil.finishContainerPrepare(war, null,
            ResourceClassProcessorPureEndPoint.class,
            ResourceClassProcessorPriiorityAImplementation.class,
            ResourceClassProcessorPriiorityBImplementation.class,
            ResourceClassProcessorPriiorityCImplementation.class);
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, ResourceClassProcessorPriorityTest.class.getSimpleName());
   }


   /**
    * @tpTestDetails Deployment uses three ResourceClassProcessors with Priority annotation,
    *                this priority annotation should be used by RESTEasy
    * @tpSince RESTEasy 3.6
    */
   @Test
   public void priorityTest() {
      // init client
      client = (QuarkusRestClient)ClientBuilder.newClient();

      // do request
      Response response = client.target(generateURL("/pure/pure")).request().get();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());

      // log visited processors
      int i = 0;
      for (String item : visitedProcessors) {
         logger.info(String.format("%d. %s", ++i, item));
      }

      // asserts
      Assert.assertThat(visitedProcessors.size(), greaterThanOrEqualTo(3));
      Assert.assertThat(visitedProcessors.get(0), is("A"));
      Assert.assertThat(visitedProcessors.get(1), is("C"));
      Assert.assertThat(visitedProcessors.get(2), is("B"));

      // close client
      client.close();
   }
}
