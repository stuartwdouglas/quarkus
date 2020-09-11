package io.quarkus.rest.test.client.other;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestClientBuilder;
import javax.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import io.quarkus.rest.test.client.other.resource.ApacheHttpClient4Resource;
import io.quarkus.rest.test.client.other.resource.ApacheHttpClient4ResourceImpl;
import io.quarkus.rest.test.client.other.resource.CustomHttpClientEngineBuilder;
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
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpTestCaseDetails Client engine customization (RESTEASY-1599)
 * @tpSince RESTEasy 3.0.24
 */
public class CustomHttpClientEngineTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(ApacheHttpClient4Resource.class);
      return TestUtil.finishContainerPrepare(war, null, ApacheHttpClient4ResourceImpl.class);
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, CustomHttpClientEngineTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Create custom ClientHttpEngine and set it to the resteasy-client
    * @tpSince RESTEasy 3.0.24
    */
   @Test
   public void test() {
      QuarkusRestClientBuilder clientBuilder = ((QuarkusRestClientBuilder)ClientBuilder.newBuilder());
      ClientHttpEngine engine = new CustomHttpClientEngineBuilder().QuarkusRestClientBuilder(clientBuilder).build();
      QuarkusRestClient client = clientBuilder.httpEngine(engine).build();
      Assert.assertTrue(ApacheHttpClient43Engine.class.isInstance(client.httpEngine()));

      ApacheHttpClient4Resource proxy = client.target(generateURL("")).proxy(ApacheHttpClient4Resource.class);
      Assert.assertEquals("Unexpected response", "hello world", proxy.get());

      client.close();
   }
}
