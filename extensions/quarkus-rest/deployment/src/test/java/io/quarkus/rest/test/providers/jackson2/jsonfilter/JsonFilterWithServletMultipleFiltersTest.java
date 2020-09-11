package io.quarkus.rest.test.providers.jackson2.jsonfilter;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.Jackson2Person;
import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.Jackson2PersonResource;
import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.ObjectFilterModifierMultiple;
import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.ObjectWriterModifierMultipleFilter;
import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.PersonType;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
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
 * @tpSubChapter Jackson2 provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Filters fields from json object. Specifies the filter implementation class in web.xml.
 * The filter filters field personType of Jackson2Person pojo. The ObjectWriterModifier has multiple filters registered.
 * Only one is set to for Json2Person pojo.
 * @tpSince RESTEasy 3.1.0
 */
public class JsonFilterWithServletMultipleFiltersTest {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClasses(Jackson2Person.class, PersonType.class, ObjectFilterModifierMultiple.class, ObjectWriterModifierMultipleFilter.class);
      war.addAsManifestResource(new StringAsset("Manifest-Version: 1.0\n" + "Dependencies: com.fasterxml.jackson.jaxrs.jackson-jaxrs-json-provider\n"), "MANIFEST.MF");
      war.addAsWebInfResource(JsonFilterWithServletMultipleFiltersTest.class.getPackage(), "web-filter-multiple.xml", "web.xml");
      return TestUtil.finishContainerPrepare(war, null, Jackson2PersonResource.class);
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, JsonFilterWithServletMultipleFiltersTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Correct filter is used when multiple filters available
    * @tpSince RESTEasy 3.1.0
    */
   @Test
   public void testJacksonString() throws Exception {
      Client client = ClientBuilder.newClient();
      WebTarget target = client.target(generateURL("/person/333"));
      Response response = target.request().get();
      response.bufferEntity();
      Assert.assertTrue("Multiple filter doesn't work", !response.readEntity(String.class).contains("id") &&
            !response.readEntity(String.class).contains("name") &&
            !response.readEntity(String.class).contains("address") &&
            response.readEntity(String.class).contains("personType"));
      client.close();
   }
}
