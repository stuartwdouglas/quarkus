package io.quarkus.rest.test.core.interceptors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.core.interceptors.resource.FilteredCookieContainerRequestFilter;
import io.quarkus.rest.test.core.interceptors.resource.FilteredCookieResource;
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
 * @tpSubChapter Cookies and filters
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1266
 * @tpSince RESTEasy 3.1.0.Final
 */
public class FilteredCookieTest {

   private static final String OLD_COOKIE_NAME = "old-cookie";
   private static final String NEW_COOKIE_NAME = "new-cookie";

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, FilteredCookieResource.class, FilteredCookieContainerRequestFilter.class);
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, FilteredCookieTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Tests if multiple cookies are returned by the server
    * @tpSince RESTEasy 3.1.0.Final
    */
   @Test
   public void testServerHeaders() {

      Client client = ClientBuilder.newClient();
      WebTarget target = client.target(generateURL("/test/get"));
      Response response = target.request().get();
      NewCookie cookie = response.getCookies().get(OLD_COOKIE_NAME);
      Assert.assertEquals(200, response.getStatus());
      Assert.assertNotNull(cookie);
      client.close();

      client = ClientBuilder.newClient();
      target = client.target(generateURL("/test/return"));
      Builder builder = target.request();
      response = builder.cookie(cookie).get();
      NewCookie oldCookie = response.getCookies().get(OLD_COOKIE_NAME);
      NewCookie newCookie = response.getCookies().get(NEW_COOKIE_NAME);
      Assert.assertEquals(200, response.getStatus());
      Assert.assertNotNull(oldCookie);
      Assert.assertNotNull(newCookie);
      client.close();
   }
}
