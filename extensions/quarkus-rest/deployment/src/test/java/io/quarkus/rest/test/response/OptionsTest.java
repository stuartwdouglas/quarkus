package io.quarkus.rest.test.response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.response.resource.OptionParamsResource;
import io.quarkus.rest.test.response.resource.OptionUsersResource;
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

import java.util.HashSet;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;


/**
 * @tpSubChapter Response
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-363
 * @tpSince RESTEasy 3.0.16
 */
public class OptionsTest {

   static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, OptionParamsResource.class, OptionUsersResource.class);
   }});

   @BeforeClass
   public static void init() {
      client = (QuarkusRestClient)ClientBuilder.newClient();
   }

   @AfterClass
   public static void close() {
      client.close();
   }

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, OptionsTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Check options HTTP request
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testOptions() throws Exception {
      WebTarget base = client.target(generateURL("/params/customers/333/phonenumbers"));
      Response response = base.request().options();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      response.close();
   }

   /**
    * @tpTestDetails Check not allowed request
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testMethodNotAllowed() throws Exception {
      WebTarget base = client.target(generateURL("/params/customers/333/phonenumbers"));
      Response response = base.request().post(Entity.text(new String()));
      Assert.assertEquals(HttpResponseCodes.SC_METHOD_NOT_ALLOWED, response.getStatus());
      response.close();

      base = client.target(generateURL("/users"));
      response = base.request().delete();
      Assert.assertEquals(HttpResponseCodes.SC_METHOD_NOT_ALLOWED, response.getStatus());
      response.close();

      base = client.target(generateURL("/users/53"));
      response = base.request().post(Entity.text(new String()));
      Assert.assertEquals(HttpResponseCodes.SC_METHOD_NOT_ALLOWED, response.getStatus());
      response.close();

      base = client.target(generateURL("/users/53/contacts"));
      response = base.request().get();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      response.close();

      base = client.target(generateURL("/users/53/contacts"));
      response = base.request().options();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      response.close();

      base = client.target(generateURL("/users/53/contacts"));
      response = base.request().delete();
      Assert.assertEquals(HttpResponseCodes.SC_METHOD_NOT_ALLOWED, response.getStatus());
      response.close();

      base = client.target(generateURL("/users/53/contacts/carl"));
      response = base.request().get();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      response.close();

      base = client.target(generateURL("/users/53/contacts/carl"));
      response = base.request().post(Entity.text(new String()));
      Assert.assertEquals(HttpResponseCodes.SC_METHOD_NOT_ALLOWED, response.getStatus());
      response.close();
   }

   /**
    * @tpTestDetails Check Allow header on 200 status
    * @tpSince RESTEasy 3.0.20
    */
   @Test
   public void testAllowHeaderOK() {
      WebTarget base = client.target(generateURL("/users/53/contacts"));
      Response response = base.request().options();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      checkOptions(response, "GET", "POST", "HEAD", "OPTIONS");
      response.close();
   }

   /**
    * @tpTestDetails Check Allow header on 405 status
    * @tpSince RESTEasy 3.0.20
    */
   @Test
   public void testAllowHeaderMethodNotAllowed() {
      WebTarget base = client.target(generateURL("/params/customers/333/phonenumbers"));
      Response response = base.request().post(Entity.text(new String()));
      Assert.assertEquals(HttpResponseCodes.SC_METHOD_NOT_ALLOWED, response.getStatus());
      checkOptions(response, "GET", "HEAD", "OPTIONS");
      response.close();
   }

   private void checkOptions(Response response, String... verbs) {
      String allowed = response.getHeaderString("Allow");
      Assert.assertNotNull(allowed);
      HashSet<String> vals = new HashSet<String>();
      for (String v : allowed.split(",")) {
         vals.add(v.trim());
      }
      Assert.assertEquals(verbs.length, vals.size());
      for (String verb : verbs) {
         Assert.assertTrue(vals.contains(verb));
      }
   }
}
