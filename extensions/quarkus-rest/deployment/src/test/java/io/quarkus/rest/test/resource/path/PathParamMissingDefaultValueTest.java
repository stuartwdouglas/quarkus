package io.quarkus.rest.test.resource.path;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.resource.path.resource.PathParamMissingDefaultValueBeanParamEntity;
import io.quarkus.rest.test.resource.path.resource.PathParamMissingDefaultValueResource;
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

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpSince RESTEasy 4.0.0
 * @tpTestCaseDetails Check for slash in URL
 */
public class PathParamMissingDefaultValueTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(PathParamMissingDefaultValueBeanParamEntity.class);
      return TestUtil.finishContainerPrepare(war, null, PathParamMissingDefaultValueResource.class);
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, PathParamMissingDefaultValueTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Missing @PathParam in BeanParam with no @DefaultValue should get java default value.
    * @tpSince RESTEasy 4.0.0
    */
   @Test
   public void testTrailingSlash() throws Exception {
      Client client = ClientBuilder.newClient();
      Response response = client.target(generateURL("/resource/test/")).request().get();
      assertEquals(200, response.getStatus());
      assertEquals("Wrong response", "nullnullnullnullnullnull", response.readEntity(String.class));
      client.close();
   }
}
