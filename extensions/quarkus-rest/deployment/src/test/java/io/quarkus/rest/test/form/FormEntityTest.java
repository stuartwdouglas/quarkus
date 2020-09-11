package io.quarkus.rest.test.form;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.form.resource.FormEntityResource;
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

/**
 * @tpSubChapter Form tests
 * @tpChapter Integration tests
 * @tpSince RESTEasy 4.0.0
 */
public class FormEntityTest {

   private static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, FormEntityResource.class);
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, FormEntityTest.class.getSimpleName());
   }

   @BeforeClass
   public static void before() throws Exception {
      client = ClientBuilder.newClient();
   }

   @AfterClass
   public static void after() throws Exception {
      client.close();
   }

   /**
    * @tpTestDetails Retrieve form param and form entity
    * @tpSince RESTEasy 4.0.0
    */
   @Test
   public void testWithEqualsAndEmptyString() throws Exception
   {
      Invocation.Builder request = client.target(generateURL("/test/form")).request();
      Response response = request.post(Entity.entity("fp=abc&fp2=\"\"", "application/x-www-form-urlencoded"));
      String s = response.readEntity(String.class);
      Assert.assertEquals(200, response.getStatus());
      Assert.assertTrue(s.equals("abc|fp=abc&fp2=\"\"") || s.equals("abc|fp2=\"\"&fp=abc"));
   }

   /**
    * @tpTestDetails Retrieve form param and form entity
    * @tpSince RESTEasy 4.0.0
    */
   @Test
   public void testWithEquals() throws Exception
   {
      Invocation.Builder request = client.target(generateURL("/test/form")).request();
      Response response = request.post(Entity.entity("fp=abc&fp2=", "application/x-www-form-urlencoded"));
      String s = response.readEntity(String.class);
      Assert.assertEquals(200, response.getStatus());
      Assert.assertTrue(s.equals("abc|fp=abc&fp2") || s.equals("abc|fp2&fp=abc"));
   }

   /**
    * @tpTestDetails Retrieve form param and form entity
    * @tpSince RESTEasy 4.0.0
    */
   @Test
   public void testWithoutEquals() throws Exception
   {
      Invocation.Builder request = client.target(generateURL("/test/form")).request();
      Response response = request.post(Entity.entity("fp=abc&fp2", "application/x-www-form-urlencoded"));
      String s = response.readEntity(String.class);
      Assert.assertEquals(200, response.getStatus());
      Assert.assertTrue(s.equals("abc|fp=abc&fp2") || s.equals("abc|fp2&fp=abc"));
   }
}
