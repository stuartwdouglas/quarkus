package io.quarkus.rest.test.resource.param;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.resource.param.resource.QueryResource;
import io.quarkus.rest.test.resource.param.resource.QuerySearchQuery;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
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
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

/**
 * @tpSubChapter Parameters
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for @Query param of the resource, RESTEASY-715
 * @tpSince RESTEasy 3.0.16
 */
public class QueryTest {

   static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, QueryResource.class, QuerySearchQuery.class);
   }});

   @BeforeClass
   public static void setup() {
      client = ClientBuilder.newClient();
   }

   @AfterClass
   public static void cleanup() {
      client.close();
   }

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, QueryTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Use resource with @Query annotation with the parameter of custom type which consist of @QueryParam fields.
    * Resteasy correctly parses the uri to get all specified parameters
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testQueryParamPrefix() throws Exception {
      WebTarget target = client.target(generateURL("/search?term=t1&order=ASC"));
      Response response = target.request().get();

      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      Assert.assertEquals("term: 't1', order: 'ASC', limit: 'null'", response.readEntity(String.class));
   }
}
