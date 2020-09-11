package io.quarkus.rest.test.validation;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.api.validation.ViolationReport;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.validation.resource.ValidationOnGetterNotNullOrOne;
import io.quarkus.rest.test.validation.resource.ValidationOnGetterNotNullOrOneStringBeanValidator;
import io.quarkus.rest.test.validation.resource.ValidationOnGetterStringBean;
import io.quarkus.rest.test.validation.resource.ValidationOnGetterValidateExecutableResource;
import io.quarkus.rest.test.validation.resource.ValidationCoreFooReaderWriter;
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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @tpSubChapter Response
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test getter validation
 * @tpSince RESTEasy 3.0.16
 */
public class ValidationOnGetterTest {
   QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, ValidationOnGetterValidateExecutableResource.class);
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
      return PortProviderUtil.generateURL(path, ValidationOnGetterTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Test xml media type.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testGetter() throws Exception {
      Response response = client.target(generateURL("/resource/executable/getter")).request().accept(MediaType.APPLICATION_XML).get();
      ViolationReport report = response.readEntity(ViolationReport.class);
      TestUtil.countViolations(report, 1, 0, 0, 0);
      response.close();
   }
}
