package io.quarkus.rest.test.core.servlet;

import org.hamcrest.CoreMatchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.core.servlet.resource.FilterForwardServlet;
import io.quarkus.rest.test.core.servlet.resource.UndertowServlet;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.utils.PermissionUtil;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
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

import java.lang.reflect.ReflectPermission;
import java.net.HttpURLConnection;
import java.net.SocketPermission;
import java.net.URL;
import java.util.PropertyPermission;

/**
 * @tpSubChapter Configuration
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-903
 * @tpSince RESTEasy 3.0.16
 */
@RunWith(Arquillian.class)
public class UndertowTest {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      // Arquillian in the deployment and use of PortProviderUtil
      war.addAsManifestResource(PermissionUtil.createPermissionsXmlAsset(new ReflectPermission("suppressAccessChecks"),
            new RuntimePermission("accessDeclaredMembers"),
            new PropertyPermission("arquillian.*", "read"),
            new PropertyPermission("node", "read"),
            new PropertyPermission("ipv6", "read"),
            new RuntimePermission("getenv.RESTEASY_PORT"),
            new PropertyPermission("org.jboss.resteasy.port", "read"),
            new SocketPermission("[" + PortProviderUtil.getHost() + "]", "connect,resolve")), "permissions.xml");
      return war;
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, "RESTEASY-903");
   }

   /**
    * @tpTestDetails Redirection in one servlet to other servlet.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testUndertow() throws Exception {
      URL url = new URL(generateURL("/test"));
      HttpURLConnection conn = HttpURLConnection.class.cast(url.openConnection());
      conn.connect();
      byte[] b = new byte[16];
      conn.getInputStream().read(b);
      Assert.assertThat("Wrong content of response", new String(b), CoreMatchers.startsWith("forward"));
      Assert.assertEquals(HttpResponseCodes.SC_OK, conn.getResponseCode());
      conn.disconnect();
   }
}
