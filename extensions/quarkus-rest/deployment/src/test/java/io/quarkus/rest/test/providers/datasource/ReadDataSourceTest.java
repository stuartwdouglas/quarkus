package io.quarkus.rest.test.providers.datasource;

import java.io.UnsupportedEncodingException;
import org.apache.http.entity.StringEntity;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.providers.datasource.resource.ReadDataSourceResource;
import org.jboss.resteasy.spi.HttpResponseCodes;
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

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

/**
 * @tpSubChapter DataSource provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class ReadDataSourceTest {

   static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, ReadDataSourceResource.class);
   }});

   @Before
   public void init() {
      client = (QuarkusRestClient)ClientBuilder.newClient();
   }

   @After
   public void after() throws Exception {
      client.close();
   }

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, ReadDataSourceTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Tests DataSourceProviders ability to read the same stream twice and verifies the results of both reads
    * are equal
    * @tpInfo RESTEASY-1182
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testDataSourceInputStream() throws Exception {
      WebTarget target = client.target(generateURL("/" + ReadDataSourceResource.PATH_UPLOAD));

      Response response = target.request().post(Entity.entity(createContent(), "text/plain"));

      final StringBuilder msg = new StringBuilder();
      final String entity = response.readEntity(String.class);
      if (entity != null) {
         msg.append("\n").append(entity);
      }
      Assert.assertEquals("Unexpected response: " + msg.toString(), HttpResponseCodes.SC_OK, response.getStatus());
   }

   private StringEntity createContent() throws UnsupportedEncodingException {
      StringBuilder stringBuilder = new StringBuilder();
      for (int i = 0; i < 2500; i++) {
         stringBuilder.append(i).append(":\n");
      }
      return new StringEntity(stringBuilder.toString());
   }
}
