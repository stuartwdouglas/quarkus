package io.quarkus.rest.test.resource.param;


import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.resource.param.resource.MultiValuedParamCdiResource;
import io.quarkus.rest.test.resource.param.resource.MultiValuedParamPersonConverterProvider;
import io.quarkus.rest.test.resource.param.resource.MultiValuedParamPersonArrayConverter;
import io.quarkus.rest.test.resource.param.resource.MultiValuedParamPersonListConverter;
import io.quarkus.rest.test.resource.param.resource.MultiValuedParamPersonSetConverter;
import io.quarkus.rest.test.resource.param.resource.MultiValuedParamPersonSortedSetConverter;
import io.quarkus.rest.test.resource.param.resource.MultiValuedParamPersonWithConverter;
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

import javax.ws.rs.core.Response;

/**
 * @tpSubChapter Parameters
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test of CDI integration for RESTEasy extended support for multivalue @*Param (RESTEASY-1566 + RESTEASY-1746)
 * @tpSince RESTEasy 4.0.0
 */
public class MultiValuedParamCdiTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(MultiValuedParamPersonWithConverter.class);
      war.addClass(MultiValuedParamPersonListConverter.class);
      war.addClass(MultiValuedParamPersonSetConverter.class);
      war.addClass(MultiValuedParamPersonSortedSetConverter.class);
      war.addClass(MultiValuedParamPersonArrayConverter.class);
      return TestUtil.finishContainerPrepare(war, null, MultiValuedParamPersonConverterProvider.class,
            MultiValuedParamCdiResource.class);
   }});

   private String generateBaseUrl() {
      return PortProviderUtil.generateBaseUrl(MultiValuedParamCdiTest.class.getSimpleName());
   }

   /**
    * Define testcase data set
    */
   static String name1 = "George";
   static String name2 = "Jack";
   static String name3 = "John";
   static MultiValuedParamPersonWithConverter person1 = new MultiValuedParamPersonWithConverter();
   static MultiValuedParamPersonWithConverter person2 = new MultiValuedParamPersonWithConverter();
   static MultiValuedParamPersonWithConverter person3 = new MultiValuedParamPersonWithConverter();
   static String expectedResponse;
   static {
      person1.setName(name1);
      person2.setName(name2);
      person3.setName(name3);
      expectedResponse = person1 + "," + person2 + "," + person3;
   }

   /**
    * @tpTestDetails Check queryParam in CDI integration
    * @tpSince RESTEasy 4.0.0
    */
   @Test
   public void testQueryParam() {
      QuarkusRestClient client = (QuarkusRestClient)ClientBuilder.newClient();
      try {
         Response response;

         response = client.target(generateBaseUrl() + "/queryParam/customConversionCdi_list")
               .queryParam("person",name1 + "," + name2 + "," + name3).request().get();
         Assert.assertEquals(expectedResponse, response.readEntity(String.class));
         response.close();

         response = client.target(generateBaseUrl() + "/queryParam/customConversionCdi_arrayList")
               .queryParam("person",name1 + "," + name2 + "," + name3).request().get();
         Assert.assertEquals(expectedResponse, response.readEntity(String.class));
         response.close();


         response = client.target(generateBaseUrl() + "/queryParam/customConversionCdi_set")
               .queryParam("person",name1 + "," + name2 + "," + name3).request().get();
         Assert.assertEquals(expectedResponse, response.readEntity(String.class));
         response.close();

         response = client.target(generateBaseUrl() + "/queryParam/customConversionCdi_hashSet")
               .queryParam("person",name1 + "," + name2 + "," + name3).request().get();
         Assert.assertEquals(expectedResponse, response.readEntity(String.class));
         response.close();

         response = client.target(generateBaseUrl() + "/queryParam/customConversionCdi_sortedSet")
               .queryParam("person",name1 + "," + name2 + "," + name3).request().get();
         Assert.assertEquals(expectedResponse, response.readEntity(String.class));
         response.close();

         response = client.target(generateBaseUrl() + "/queryParam/customConversionCdi_treeSet")
               .queryParam("person",name1 + "," + name2 + "," + name3).request().get();
         Assert.assertEquals(expectedResponse, response.readEntity(String.class));
         response.close();


         response = client.target(generateBaseUrl() + "/queryParam/customConversionCdi_array")
               .queryParam("person",name1 + "," + name2 + "," + name3).request().get();
         Assert.assertEquals(expectedResponse, response.readEntity(String.class));
         response.close();

      } finally {
         client.close();
      }
   }
}
