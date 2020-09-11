package io.quarkus.rest.test.form;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.form.resource.NestedCollectionsFormAddress;
import io.quarkus.rest.test.form.resource.NestedCollectionsFormCountry;
import io.quarkus.rest.test.form.resource.NestedCollectionsFormPerson;
import io.quarkus.rest.test.form.resource.NestedCollectionsFormResource;
import io.quarkus.rest.test.form.resource.NestedCollectionsFormTelephoneNumber;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @tpSubChapter Resources
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test nested form parameters
 * @tpSince RESTEasy 3.0.16
 */
public class NestedCollectionsFormTest {

   static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(NestedCollectionsFormTelephoneNumber.class);
      war.addClass(NestedCollectionsFormPerson.class);
      war.addClass(NestedCollectionsFormCountry.class);
      war.addClass(NestedCollectionsFormAddress.class);
      return TestUtil.finishContainerPrepare(war, null, NestedCollectionsFormResource.class);
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, NestedCollectionsFormTest.class.getSimpleName());
   }

   @Before
   public void init() {
      client = (QuarkusRestClient)ClientBuilder.newClient();
   }

   @After
   public void close() {
      client.close();
   }

   /**
    * @tpTestDetails Set all relevant parameters to form.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void shouldSupportCollectionsWithNestedObjectsInForm() throws Exception {
      javax.ws.rs.core.Form form = new javax.ws.rs.core.Form()
         .param("telephoneNumbers[0].country.code", "31")
         .param("telephoneNumbers[0].number", "0612345678")
         .param("telephoneNumbers[1].country.code", "91")
         .param("telephoneNumbers[1].number", "9717738723")
         .param("address[INVOICE].street", "Main Street")
         .param("address[INVOICE].houseNumber", "2")
         .param("address[INVOICE].country.code", "NL")
         .param("address[SHIPPING].street", "Square One")
         .param("address[SHIPPING].houseNumber", "13")
         .param("address[SHIPPING].country.code", "IN");

      WebTarget base = client.target(PortProviderUtil.generateURL("/person", NestedCollectionsFormTest.class.getSimpleName()));
      Response response = base.request().accept(MediaType.TEXT_PLAIN).post(Entity.form(form));

      Assert.assertEquals(HttpResponseCodes.SC_NO_CONTENT, response.getStatus());

      response.close();
   }
}
