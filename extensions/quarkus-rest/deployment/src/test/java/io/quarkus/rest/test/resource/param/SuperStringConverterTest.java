package io.quarkus.rest.test.resource.param;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.client.jaxrs.ProxyBuilder;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.resource.param.resource.SuperStringConverterCompany;
import io.quarkus.rest.test.resource.param.resource.SuperStringConverterCompanyConverter;
import io.quarkus.rest.test.resource.param.resource.SuperStringConverterCompanyConverterProvider;
import io.quarkus.rest.test.resource.param.resource.SuperStringConverterMyClient;
import io.quarkus.rest.test.resource.param.resource.SuperStringConverterObjectConverter;
import io.quarkus.rest.test.resource.param.resource.SuperStringConverterPerson;
import io.quarkus.rest.test.resource.param.resource.SuperStringConverterPersonConverter;
import io.quarkus.rest.test.resource.param.resource.SuperStringConverterPersonConverterProvider;
import io.quarkus.rest.test.resource.param.resource.SuperStringConverterResource;
import io.quarkus.rest.test.resource.param.resource.SuperStringConverterSuperPersonConverter;
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
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 * @tpTestCaseDetails Test for org.jboss.resteasy.spi.StringConverter class
 *                    StringConverter is deprecated.
 *                    See javax.ws.rs.ext.ParamConverter
 *                    See io.quarkus.rest.test.resource.param.ParamConverterTest
 */
public class SuperStringConverterTest {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(SuperStringConverterPerson.class);
      war.addClass(SuperStringConverterObjectConverter.class);
      war.addClass(SuperStringConverterSuperPersonConverter.class);
      war.addClass(SuperStringConverterPersonConverterProvider.class);
      war.addClass(SuperStringConverterMyClient.class);
      war.addClass(SuperStringConverterCompany.class);
      war.addClass(SuperStringConverterCompanyConverterProvider.class);
      return TestUtil.finishContainerPrepare(war, null, SuperStringConverterPersonConverter.class,
            SuperStringConverterCompanyConverter.class, SuperStringConverterCompanyConverterProvider.class,
            SuperStringConverterResource.class);
   }});

   private String generateBaseUrl() {
      return PortProviderUtil.generateBaseUrl(SuperStringConverterTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Test converter on basic object
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testPerson() throws Exception {
      QuarkusRestClient client = (QuarkusRestClient)ClientBuilder.newClient();
      client.register(SuperStringConverterPersonConverterProvider.class);
      client.register(SuperStringConverterCompanyConverterProvider.class);

      SuperStringConverterMyClient proxy = ProxyBuilder.builder(SuperStringConverterMyClient.class, client.target(generateBaseUrl())).build();
      SuperStringConverterPerson person = new SuperStringConverterPerson("name");
      proxy.put(person);
      client.close();
   }

   /**
    * @tpTestDetails Test converter on object with override on "toString" method
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testCompany() throws Exception {
      QuarkusRestClient client = (QuarkusRestClient)ClientBuilder.newClient();
      client.register(SuperStringConverterPersonConverterProvider.class);
      client.register(SuperStringConverterCompanyConverterProvider.class);
      SuperStringConverterMyClient proxy = ProxyBuilder.builder(SuperStringConverterMyClient.class, client.target(generateBaseUrl())).build();
      SuperStringConverterCompany company = new SuperStringConverterCompany("name");
      proxy.putCompany(company);
      client.close();
   }
}
