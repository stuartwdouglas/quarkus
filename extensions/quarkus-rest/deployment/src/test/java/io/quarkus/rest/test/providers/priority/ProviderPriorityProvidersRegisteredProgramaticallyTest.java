package io.quarkus.rest.test.providers.priority;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import io.quarkus.rest.test.providers.priority.resource.ProviderPriorityExceptionMapperAAA;
import io.quarkus.rest.test.providers.priority.resource.ProviderPriorityExceptionMapperBBB;
import io.quarkus.rest.test.providers.priority.resource.ProviderPriorityExceptionMapperCCC;
import io.quarkus.rest.test.providers.priority.resource.ProviderPriorityFoo;
import io.quarkus.rest.test.providers.priority.resource.ProviderPriorityFooParamConverter;
import io.quarkus.rest.test.providers.priority.resource.ProviderPriorityFooParamConverterProviderAAA;
import io.quarkus.rest.test.providers.priority.resource.ProviderPriorityFooParamConverterProviderBBB;
import io.quarkus.rest.test.providers.priority.resource.ProviderPriorityFooParamConverterProviderCCC;
import io.quarkus.rest.test.providers.priority.resource.ProviderPriorityResource;
import io.quarkus.rest.test.providers.priority.resource.ProviderPriorityTestException;
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

/**
 * @tpSubChapter ExceptionMappers and ParamConverterProviders registered programatically
 * @tpChapter Integration tests
 * @tpSince RESTEasy 4.0.0
 */
public class ProviderPriorityProvidersRegisteredProgramaticallyTest {

   static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClasses(ProviderPriorityFoo.class,
            ProviderPriorityFooParamConverter.class,
            ProviderPriorityTestException.class,
            ProviderPriorityExceptionMapperCCC.class,
            ProviderPriorityFooParamConverterProviderCCC.class
      );
      List<Class<?>> singletons = new ArrayList<Class<?>>();
      singletons.add(ProviderPriorityExceptionMapperCCC.class);
      singletons.add(ProviderPriorityFooParamConverterProviderCCC.class);
      return TestUtil.finishContainerPrepare(war, null, singletons,
            ProviderPriorityResource.class,
            ProviderPriorityExceptionMapperAAA.class,
            ProviderPriorityExceptionMapperBBB.class,
            ProviderPriorityFooParamConverterProviderAAA.class,
            ProviderPriorityFooParamConverterProviderBBB.class
            );
   }});

   private ResteasyProviderFactory factory;
   @Before
   public void init() {
      factory = ResteasyProviderFactory.newInstance();
      RegisterBuiltin.register(factory);
      ResteasyProviderFactory.setInstance(factory);

      client = (QuarkusRestClient)ClientBuilder.newClient();
   }

   @After
   public void after() throws Exception {
      client.close();
      // Clear the singleton
      ResteasyProviderFactory.clearInstanceIfEqual(factory);
   }

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, ProviderPriorityProvidersRegisteredProgramaticallyTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Tests that Programatically registered ExceptionMappers and
    *                ParamConverterProviders are sorted by priority
    * @tpSince RESTEasy 4.0.0
    */
   @Test
   public void testProgramaticRegistration() throws Exception {
      WebTarget base = client.target(generateURL(""));
      base.path("/register");
      Response response = base.path("/register").request().get();
      assertEquals(200, response.getStatus());
      assertEquals("ok", response.readEntity(String.class));

      response = base.path("/exception").request().get();
      assertEquals(444, response.getStatus());
      assertEquals("CCC", response.readEntity(String.class));

      response = base.path("/paramconverter/dummy").request().get();
      assertEquals(200, response.getStatus());
      assertEquals("CCC", response.readEntity(String.class));
   }
}
