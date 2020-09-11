package io.quarkus.rest.test.resource.path;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.resource.path.resource.ResteasyTrailingSlashResource;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
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

import static org.junit.Assert.assertEquals;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 * @tpTestCaseDetails Check for slash in URL
 */
public class ResteasyTrailingSlashTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, ResteasyTrailingSlashResource.class);
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, ResteasyTrailingSlashTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Client should accept also URL ended by slash
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testTrailingSlash() throws Exception {
      Client client = ClientBuilder.newClient();
      String val = client.target(generateURL("/test/"))
            .request().get(String.class);
      assertEquals("Wrong response", "hello world", val);
      client.close();
   }
}
