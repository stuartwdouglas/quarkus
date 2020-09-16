package io.quarkus.rest.test.cdi.basic;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.cdi.basic.resource.ApplicationInjection;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for injecting of Application
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Application Injection Test")
public class ApplicationInjectionTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            // Arquillian in the deployment
            war.addClass(ApplicationInjection.class);
            return war;
        }
    });

    /**
     * @tpTestDetails Injected application instance should not be null.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test App Injection")
    public void testAppInjection() throws Exception {
        Assertions.assertEquals(1, ApplicationInjection.instances.size(), "Wrong count of initialized applications");
        ApplicationInjection app = ApplicationInjection.instances.iterator().next();
        Assertions.assertNotNull(app.app, "Injected application instance should not be null");
    }
}
