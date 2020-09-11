package io.quarkus.rest.test.validation;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.api.validation.ViolationReport;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.validation.resource.ValidationCoreFooReaderWriter;
import io.quarkus.rest.test.validation.resource.ValidationHibernateI18NResource;
import org.jboss.resteasy.util.HttpHeaderNames;
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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.hamcrest.core.StringStartsWith.startsWith;

/**
 * @tpSubChapter Response
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for internationalization of hibernate validator
 * @tpSince RESTEasy 3.0.16
 */
public class ValidationHibernateI18NTest {
   static final String WRONG_ERROR_MSG = "Expected validation error is not in response";
   QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, ValidationHibernateI18NResource.class);
   }});

   @Before
   public void init() {
      client = (QuarkusRestClient)ClientBuilder.newClient().register(ValidationCoreFooReaderWriter.class);
   }

   @After
   public void after() throws Exception {
      client.close();
   }

   private static String generateURL(String path) {
      return PortProviderUtil.generateURL(path, ValidationHibernateI18NTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Test two languages.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testI18NSetAcceptLanguage() throws Exception {
      doTestI18NSetAcceptLanguage("fr", "la taille doit être");
      doTestI18NSetAcceptLanguage("es", "el tama\u00F1o tiene que estar entre");
   }

   protected void doTestI18NSetAcceptLanguage(String locale, String expectedMessage) throws Exception {
      Response response = client.target(generateURL("/test")).request()
            .accept(MediaType.APPLICATION_XML).header(HttpHeaderNames.ACCEPT_LANGUAGE, locale).get();

      ViolationReport report = response.readEntity(ViolationReport.class);
      String message = report.getReturnValueViolations().iterator().next().getMessage();
      TestUtil.countViolations(report, 0, 0, 0, 1);
      Assert.assertThat(WRONG_ERROR_MSG, message, startsWith(expectedMessage));
      response.close();
   }
}
