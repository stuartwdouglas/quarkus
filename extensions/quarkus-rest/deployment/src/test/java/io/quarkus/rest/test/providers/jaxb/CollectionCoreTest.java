package io.quarkus.rest.test.providers.jaxb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.providers.jaxb.resource.CollectionCustomer;
import io.quarkus.rest.test.providers.jaxb.resource.CollectionNamespacedResource;
import io.quarkus.rest.test.providers.jaxb.resource.CollectionResource;
import io.quarkus.rest.test.providers.jaxb.resource.CollectionNamespacedCustomer;
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
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.containsString;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Check jaxb requests with collection
 * @tpSince RESTEasy 3.0.16
*/
public class CollectionCoreTest {
   private static final String WRONG_RESPONSE = "Response contains wrong data";
   protected static final Logger logger = LogManager.getLogger(CollectionCoreTest.class.getName());

   static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClasses(CollectionCustomer.class, CollectionNamespacedCustomer.class);
      return TestUtil.finishContainerPrepare(war, null, CollectionResource.class, CollectionNamespacedResource.class);
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
      return PortProviderUtil.generateURL(path, CollectionCoreTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Test array response
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testArray() throws Exception {
      Invocation.Builder request = client.target(generateURL("/array")).request();
      Response response = request.get();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      String str = response.readEntity(String.class);
      logger.info(String.format("Response: %s", str));
      response.close();
      response = request.put(Entity.entity(str, "application/xml"));
      Assert.assertEquals(HttpResponseCodes.SC_NO_CONTENT, response.getStatus());
      response.close();
   }

   /**
    * @tpTestDetails Test list response
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testList() throws Exception {
      Invocation.Builder request = client.target(generateURL("/list")).request();
      Response response = request.get();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      String str = response.readEntity(String.class);
      logger.info(String.format("Response: %s", str));
      response.close();
      response = request.put(Entity.entity(str, "application/xml"));
      Assert.assertEquals(HttpResponseCodes.SC_NO_CONTENT, response.getStatus());
      response.close();
   }

   /**
    * @tpTestDetails Test GenericEntity of list response
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testResponse() throws Exception {
      Invocation.Builder request = client.target(generateURL("/list/response")).request();
      Response response = request.get();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      logger.info(String.format("Response: %s", response.readEntity(String.class)));
   }

   /**
    * @tpTestDetails Test array of customers with namespace in XML
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testNamespacedArray() throws Exception {
      Invocation.Builder request = client.target(generateURL("/namespaced/array")).request();
      Response response = request.get();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      String str = response.readEntity(String.class);
      logger.info(String.format("Response: %s", str));
      response.close();
      response = request.put(Entity.entity(str, "application/xml"));
      Assert.assertEquals(HttpResponseCodes.SC_NO_CONTENT, response.getStatus());
      response.close();
      Assert.assertThat(WRONG_RESPONSE, str, containsString("http://customer.com"));
   }

   /**
    * @tpTestDetails Test GenericEntity with list of customers with namespace in XML
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testNamespacedList() throws Exception {
      Invocation.Builder request = client.target(generateURL("/namespaced/list")).request();
      Response response = request.get();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      String str = response.readEntity(String.class);
      logger.info(String.format("Response: %s", str));
      response.close();
      response = request.put(Entity.entity(str, "application/xml"));
      Assert.assertEquals(HttpResponseCodes.SC_NO_CONTENT, response.getStatus());
      response.close();
      Assert.assertThat(WRONG_RESPONSE, str, containsString("http://customer.com"));
   }

   /**
    * @tpTestDetails Test list of customers with namespace in XML
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testNamespacedResponse() throws Exception {
      Invocation.Builder request = client.target(generateURL("/namespaced/list/response")).request();
      Response response = request.get();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      String str = response.readEntity(String.class);
      logger.info(String.format("Response: %s", str));
      response.close();
      Assert.assertThat(WRONG_RESPONSE, str, containsString("http://customer.com"));
   }

}
