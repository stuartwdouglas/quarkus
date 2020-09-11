package io.quarkus.rest.test.resource.param;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import org.jboss.resteasy.client.jaxrs.internal.QuarkusRestClientBuilderImpl;
import io.quarkus.rest.test.resource.param.resource.ParamConverterClient;
import io.quarkus.rest.test.resource.param.resource.ParamConverterDefaultClient;
import io.quarkus.rest.test.resource.param.resource.ParamConverterDefaultIntegerResource;
import io.quarkus.rest.test.resource.param.resource.ParamConverterIntegerConverter;
import io.quarkus.rest.test.resource.param.resource.ParamConverterIntegerConverterProvider;
import io.quarkus.rest.test.resource.param.resource.ParamConverterIntegerResource;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import io.quarkus.rest.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.rest.test.simple.TestUtil;

/**
 * @tpSubChapter Parameters
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for ParamConverter RESTEASY-2222
 * @tpSince RESTEasy 3.7.0
 */
public class PrimitiveParamConverterTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(ParamConverterIntegerConverter.class);
      war.addClass(ParamConverterDefaultClient.class);
      war.addClass(ParamConverterClient.class);
      return TestUtil.finishContainerPrepare(war, null, ParamConverterIntegerConverterProvider.class,
            ParamConverterIntegerResource.class, ParamConverterDefaultIntegerResource.class);
   }});

   private String generateBaseUrl() {
      return PortProviderUtil.generateBaseUrl(PrimitiveParamConverterTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Set specific values
    * @tpSince RESTEasy 3.7.0
    */
   @Test
   public void testIt() throws Exception {
      QuarkusRestClient client = new QuarkusRestClientBuilderImpl().build();
      ParamConverterClient proxy = client.target(generateBaseUrl()).proxy(ParamConverterClient.class);
      proxy.put("4", "4", "4", "4");
      client.close();
   }

   /**
    * @tpTestDetails Check default values
    * @tpSince RESTEasy 3.7.0
    */
   @Test
   public void testDefault() throws Exception {
      QuarkusRestClient client = new QuarkusRestClientBuilderImpl().build();
      ParamConverterDefaultClient proxy = client.target(generateBaseUrl()).proxy(ParamConverterDefaultClient.class);
      proxy.put();
      client.close();
   }
}
