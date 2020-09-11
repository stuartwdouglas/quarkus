package io.quarkus.rest.test.core.interceptors;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.plugins.interceptors.CorsFilter;
import org.jboss.resteasy.spi.CorsHeaders;
import io.quarkus.rest.test.core.interceptors.resource.CorsFiltersResource;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.utils.PermissionUtil;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestApplication;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
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
import java.net.SocketPermission;
import java.util.ArrayList;
import java.util.List;
import java.util.PropertyPermission;
import java.util.logging.LoggingPermission;

import static org.hamcrest.core.Is.is;

/**
 * @tpSubChapter Interceptors
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test CorsFilter usage
 * @tpSince RESTEasy 3.0.16
 */
@RunWith(Arquillian.class)
public class CorsFiltersTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(PortProviderUtil.class);

      List<Class<?>> singletons = new ArrayList<>();
      singletons.add(CorsFilter.class);
      // Arquillian in the deployment and use of PortProviderUtil

      return TestUtil.finishContainerPrepare(war, null, singletons, CorsFiltersResource.class);
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, CorsFiltersTest.class.getSimpleName());
   }

   @After
   public void resetFilter() {
      CorsFilter corsFilter = (CorsFilter) TestApplication.singletons.iterator().next();
      corsFilter.getAllowedOrigins().remove("http://" + PortProviderUtil.getHost());
   }

   /**
    * @tpTestDetails Check different options of Cors headers.
    * CorsFilter is created as singleton in TestApplication instance.
    * In this test is CorsFilter get from static set from TestApplication class.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testPreflight() throws Exception {
      String testedURL = "http://" + PortProviderUtil.getHost();

      QuarkusRestClient client = (QuarkusRestClient)ClientBuilder.newClient();
      WebTarget target = client.target(generateURL("/test"));
      Response response = target.request().header(CorsHeaders.ORIGIN, testedURL)
            .options();
      Assert.assertEquals(HttpResponseCodes.SC_FORBIDDEN, response.getStatus());
      response.close();
      response = target.request().header(CorsHeaders.ORIGIN, testedURL)
            .get();
      Assert.assertEquals(HttpResponseCodes.SC_FORBIDDEN, response.getStatus());
      response.close();

      Assert.assertThat("Wrong count of singletons were created", TestApplication.singletons.size(), is(1));
      CorsFilter corsFilter = (CorsFilter) TestApplication.singletons.iterator().next();

      corsFilter.getAllowedOrigins().add(testedURL);
      response = target.request().header(CorsHeaders.ORIGIN, testedURL)
            .options();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      response.close();
      response = target.request().header(CorsHeaders.ORIGIN, testedURL)
            .get();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      Assert.assertEquals(response.getHeaderString(CorsHeaders.ACCESS_CONTROL_ALLOW_ORIGIN), testedURL);
      Assert.assertEquals("Wrong response", "hello", response.readEntity(String.class));
      response.close();

      client.close();
   }

   /**
    * @tpTestDetails Test that the response contains the Vary: Origin header
    * @tpInfo RESTEASY-1704
    * @tpSince RESTEasy 3.0.25
    */
   @Test
   public void testVaryOriginHeader() {
      String testedURL = "http://" + PortProviderUtil.getHost();
      QuarkusRestClient client = (QuarkusRestClient)ClientBuilder.newClient();
      WebTarget target = client.target(generateURL("/test"));

      Assert.assertThat("Wrong count of singletons were created", TestApplication.singletons.size(), is(1));
      CorsFilter corsFilter = (CorsFilter) TestApplication.singletons.iterator().next();
      corsFilter.getAllowedOrigins().add(testedURL);

      Response response = target.request().header(CorsHeaders.ORIGIN, testedURL).get();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      Assert.assertEquals("Response doesn't contain the Vary: Origin header", CorsHeaders.ORIGIN, response.getHeaderString(CorsHeaders.VARY));
      response.close();

      response = target.request().header(CorsHeaders.ORIGIN, testedURL).options();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      Assert.assertEquals("Response doesn't contain the Vary: Origin header", CorsHeaders.ORIGIN, response.getHeaderString(CorsHeaders.VARY));
      response.close();

      client.close();
   }

}
