package io.quarkus.rest.test.form;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.form.resource.FormParameterResource;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
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
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @tpSubChapter Form tests
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-760
 * @tpSince RESTEasy 3.0.16
 */
public class FormParameterTest {

   static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClasses(FormParameterTest.class);
      return TestUtil.finishContainerPrepare(war, null, FormParameterResource.class);
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, FormParameterTest.class.getSimpleName());
   }

   @Before
   public void init() {
      client = (QuarkusRestClient)ClientBuilder.newClient();
   }

   @After
   public void after() throws Exception {
      client.close();
      client = null;
   }

   /**
    * @tpTestDetails Client sends PUT requests.
    *      Form parameter is used and should be returned by RE resource.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testFormParamWithNoQueryParamPut() throws Exception {
      Form form = new Form();
      form.param("formParam", "abc xyz");
      Response response = client.target(generateURL("/put/noquery/")).request()
            .header("Content-Type", "application/x-www-form-urlencoded")
            .put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
      assertThat("Wrong response", response, notNullValue());

      response.bufferEntity();
      assertEquals("Wrong response", "abc xyz", response.readEntity(String.class));
   }

   /**
    * @tpTestDetails Client sends PUT requests.
    *      Form parameter is used (encoded) and should be returned by RE resource.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testFormParamWithNoQueryParamPutEncoded() throws Exception {
      Form form = new Form();
      form.param("formParam", "abc xyz");
      Response response = client.target(generateURL("/put/noquery/encoded")).request()
            .header("Content-Type", "application/x-www-form-urlencoded")
            .put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

      assertThat("Wrong response", response, notNullValue());
      response.bufferEntity();
      assertEquals("Wrong response", "abc+xyz", response.readEntity(String.class));
   }

   /**
    * @tpTestDetails Client sends POST requests.
    *      Form parameter is used and should be returned by RE resource.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testFormParamWithNoQueryParamPost() throws Exception {
      Form form = new Form();
      form.param("formParam", "abc xyz");
      Response response = client.target(generateURL("/post/noquery/")).request()
            .header("Content-Type", "application/x-www-form-urlencoded")
            .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

      assertThat("Wrong response", response, notNullValue());
      response.bufferEntity();
      assertEquals("Wrong response", "abc xyz", response.readEntity(String.class));
   }

   /**
    * @tpTestDetails Client sends POST requests.
    *      Form parameter is used (encoded) and should be returned by RE resource.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testFormParamWithNoQueryParamPostEncoded() throws Exception {
      Form form = new Form();
      form.param("formParam", "abc xyz");
      Response response = client.target(generateURL("/post/noquery/encoded")).request()
            .header("Content-Type", "application/x-www-form-urlencoded")
            .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

      assertThat("Wrong response", response, notNullValue());
      response.bufferEntity();
      assertEquals("Wrong response", "abc+xyz", response.readEntity(String.class));
   }

   /**
    * @tpTestDetails Client sends PUT requests. Query parameter is used.
    *      Form parameter is used too and should be returned by RE resource.
    *      This is regression test for JBEAP-982
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testFormParamWithQueryParamPut() throws Exception {
      Form form = new Form();
      form.param("formParam", "abc xyz");
      Response response = client.target(generateURL("/put/query?query=xyz")).request()
            .header("Content-Type", "application/x-www-form-urlencoded")
            .put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

      assertThat("Wrong response", response, notNullValue());
      response.bufferEntity();
      assertEquals("Wrong response", "abc xyz", response.readEntity(String.class));
   }

   /**
    * @tpTestDetails Client sends PUT requests. Query parameter is used.
    *      Form parameter is used too (encoded) and should be returned by RE resource.
    *      This is regression test for JBEAP-982
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testFormParamWithQueryParamPutEncoded() throws Exception {
      Form form = new Form();
      form.param("formParam", "abc xyz");
      Response response = client.target(generateURL("/put/query/encoded?query=xyz")).request()
            .header("Content-Type", "application/x-www-form-urlencoded")
            .put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

      assertThat("Wrong response", response, notNullValue());
      response.bufferEntity();
      assertEquals("Wrong response", "abc+xyz", response.readEntity(String.class));
   }

   /**
    * @tpTestDetails Client sends POST requests. Query parameter is used.
    *      Form parameter is used too and should be returned by RE resource.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testFormParamWithQueryParamPost() throws Exception {
      Form form = new Form();
      form.param("formParam", "abc xyz");
      Response response = client.target(generateURL("/post/query?query=xyz")).request()
            .header("Content-Type", "application/x-www-form-urlencoded")
            .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

      assertThat("Wrong response", response, notNullValue());
      response.bufferEntity();
      assertEquals("Wrong response", "abc xyz", response.readEntity(String.class));
   }

   /**
    * @tpTestDetails Client sends POST requests. Query parameter is used.
    *      Form parameter is used too (encoded) and should be returned by RE resource.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testFormParamWithQueryParamPostEncoded() throws Exception {
      Form form = new Form();
      form.param("formParam", "abc xyz");
      Response response = client.target(generateURL("/post/query/encoded?query=xyz")).request()
            .header("Content-Type", "application/x-www-form-urlencoded")
            .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

      assertThat("Wrong response", response, notNullValue());
      response.bufferEntity();
      assertEquals("Wrong response", "abc+xyz", response.readEntity(String.class));
   }
}
