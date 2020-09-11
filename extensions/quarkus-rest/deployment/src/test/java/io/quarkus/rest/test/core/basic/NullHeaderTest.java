package io.quarkus.rest.test.core.basic;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.core.basic.resource.NullHeaderFilter;
import io.quarkus.rest.test.core.basic.resource.NullHeaderResource;
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

/**
 * @tpSubChapter RESTEASY-1565
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.1.1.Final
 */
public class NullHeaderTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(NullHeaderFilter.class);
      return TestUtil.finishContainerPrepare(war, null, NullHeaderResource.class);
   }});

   @Test
   public void testNullHeader() throws Exception {

      Client client = ClientBuilder.newClient();
      WebTarget base = client.target(PortProviderUtil.generateURL("/test", NullHeaderTest.class.getSimpleName()));
      Response response = base.register(NullHeaderFilter.class).request().header("X-Auth-User", null).get();
      Assert.assertNotNull(response);
      Assert.assertEquals(200, response.getStatus());
      String serverHeader = response.getHeaderString("X-Server-Header");
      Assert.assertTrue(serverHeader == null || "".equals(serverHeader));
      client.close();
   }
}
