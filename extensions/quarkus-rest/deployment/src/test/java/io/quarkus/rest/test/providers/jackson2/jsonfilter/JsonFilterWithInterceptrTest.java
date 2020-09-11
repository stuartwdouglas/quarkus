package io.quarkus.rest.test.providers.jackson2.jsonfilter;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.Jackson2Product;
import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.Jackson2Resource;
import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.JsonFilterWriteInterceptor;
import io.quarkus.rest.test.providers.jackson2.jsonfilter.resource.ObjectFilterModifier;
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
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 * @tpSubChapter Jackson2 provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.1.0
 */
public class JsonFilterWithInterceptrTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClasses(Jackson2Product.class, ObjectFilterModifier.class);
      war.addAsManifestResource(new StringAsset("Manifest-Version: 1.0\n" + "Dependencies: com.fasterxml.jackson.jaxrs.jackson-jaxrs-json-provider\n"), "MANIFEST.MF");
      return TestUtil.finishContainerPrepare(war, null, Jackson2Resource.class, JsonFilterWriteInterceptor.class);
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, JsonFilterWithInterceptrTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Filters fields from json response entity using interceptor
    * @tpSince RESTEasy 3.1.0
    */
   @Test
   public void testJacksonString() throws Exception {
      Client client = ClientBuilder.newClient();
      WebTarget target = client.target(generateURL("/products/333"));
      Response response = target.request().get();
      response.bufferEntity();
      Assert.assertTrue("filter doesn't work", !response.readEntity(String.class).contains("id") &&
            response.readEntity(String.class).contains("name"));
      client.close();
   }
}
