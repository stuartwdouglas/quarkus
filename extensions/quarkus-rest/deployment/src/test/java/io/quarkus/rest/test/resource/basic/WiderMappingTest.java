package io.quarkus.rest.test.resource.basic;


import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.resource.basic.resource.WiderMappingDefaultOptions;
import io.quarkus.rest.test.resource.basic.resource.WiderMappingResource;
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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * @tpSubChapter Resources
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test positive scenario for "resteasy.wider.request.matching" property
 * @tpSince RESTEasy 3.0.16
 */
public class WiderMappingTest {

   static Client client;

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, WiderMappingNegativeTest.class.getSimpleName());
   }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(PortProviderUtil.class);

      Map<String, String> contextParam = new HashMap<>();
      contextParam.put("resteasy.wider.request.matching", "true");
      return TestUtil.finishContainerPrepare(war, contextParam, WiderMappingResource.class, WiderMappingDefaultOptions.class);
   }});


   @BeforeClass
   public static void setup() {
      client = ClientBuilder.newClient();
   }

   @AfterClass
   public static void cleanup() {
      client.close();
   }

   /**
    * @tpTestDetails Two resources used, more general resource should be used
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testOptions() {
      Response response = client.target(generateURL("/hello/int")).request().options();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      Assert.assertEquals(response.readEntity(String.class), "hello");
      response.close();
   }

}
