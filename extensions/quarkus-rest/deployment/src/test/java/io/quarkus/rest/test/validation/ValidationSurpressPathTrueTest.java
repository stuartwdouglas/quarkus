package io.quarkus.rest.test.validation;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.validation.resource.ValidationCoreFoo;
import io.quarkus.rest.test.validation.resource.ValidationCoreFooConstraint;
import io.quarkus.rest.test.validation.resource.ValidationCoreFooReaderWriter;
import io.quarkus.rest.test.validation.resource.ValidationCoreFooValidator;
import io.quarkus.rest.test.validation.resource.ValidationCoreClassConstraint;
import io.quarkus.rest.test.validation.resource.ValidationCoreClassValidator;
import io.quarkus.rest.test.validation.resource.ValidationCoreResourceWithAllViolationTypes;
import io.quarkus.rest.test.validation.resource.ValidationCoreResourceWithReturnValues;
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

import java.util.HashMap;
import java.util.Map;

/**
 * @tpSubChapter Response
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for true value of resteasy.validation.suppress.path parameter
 * @tpSince RESTEasy 3.0.16
 */
public class ValidationSurpressPathTrueTest extends ValidationSuppressPathTestBase {
   @SuppressWarnings(value = "unchecked")
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      Map<String, String> contextParams = new HashMap<>();
      contextParams.put("resteasy.validation.suppress.path", "true");
      return TestUtil.finishContainerPrepare(war, contextParams, (Class<?>[]) null);
   }});

   /**
    * @tpTestDetails Test input violations.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testInputViolations() throws Exception {
      doTestInputViolations("*", "*", "*", "*");
   }

   /**
    * @tpTestDetails Test return value violations.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testReturnValues() throws Exception {
      doTestReturnValueViolations("*");
   }
}
