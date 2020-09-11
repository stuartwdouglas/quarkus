package io.quarkus.rest.test.core.servlet;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import io.quarkus.rest.test.core.servlet.resource.ServletMappingProxy;
import io.quarkus.rest.test.core.basic.resource.MyFilter;
import io.quarkus.rest.test.core.servlet.resource.ServletMappingResource;
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

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.lang.reflect.ReflectPermission;

/**
 * @tpSubChapter Configuration
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for servlet mapping. This settings is in web.xml
 * @tpSince RESTEasy 3.0.16
 */
public class ServletMappingTest {
   public static final String WRONG_RESPONSE_ERROR_MSG = "Wrong content of response";

   private static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addAsWebInfResource(ServletMappingTest.class.getPackage(), "ServletMappingWeb.xml", "web.xml");
      war.addAsWebInfResource(ServletMappingTest.class.getPackage(), "ServletMappingJbossWeb.xml", "jboss-web.xml");
      war.addClass(MyFilter.class);

      return TestUtil.finishContainerPrepare(war, null, ServletMappingResource.class, ServletMappingProxy.class);
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, "");
   }

   @Before
   public void setup() {
      client = (QuarkusRestClient)ClientBuilder.newClient();
   }

   @After
   public void after() throws Exception {
      client.close();
   }

   /**
    * @tpTestDetails Test for new resteasy client without proxy
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testNoDefaultsResourceNewQuarkusRestClient() throws Exception {
      WebTarget target = client.target(generateURL("/resteasy/rest/basic"));
      Response response = target.request().get();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      Assert.assertEquals(WRONG_RESPONSE_ERROR_MSG, "basic", response.readEntity(String.class));
      response.close();
   }

   /**
    * @tpTestDetails Test for apache client
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testNoDefaultsResourceApacheClient() throws Exception {
      CloseableHttpClient httpclient = HttpClients.createDefault();
      HttpGet httpGet = new HttpGet(generateURL("/resteasy/rest/basic"));
      CloseableHttpResponse response1 = httpclient.execute(httpGet);

      try {
         Assert.assertEquals(HttpResponseCodes.SC_OK, response1.getStatusLine().getStatusCode());
         Assert.assertEquals(WRONG_RESPONSE_ERROR_MSG, "basic", TestUtil.readString(response1.getEntity().getContent()));
      } finally {
         response1.close();
      }
   }

   /**
    * @tpTestDetails Test for new resteasy client with proxy
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testFormParamNewQuarkusRestClient() {
      ResteasyWebTarget target = client.target(generateURL("/resteasy/rest"));
      ServletMappingProxy client = target.proxyBuilder(ServletMappingProxy.class).build();
      final String result = client.postForm("value");
      Assert.assertEquals(WRONG_RESPONSE_ERROR_MSG, result, "value");
   }
}
