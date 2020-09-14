package io.quarkus.rest.test.cdi.basic;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
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

public class ApplicationInjectionTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
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
    public void testAppInjection() throws Exception {
        Assert.assertEquals("Wrong count of initialized applications", 1, ApplicationInjection.instances.size());
        ApplicationInjection app = ApplicationInjection.instances.iterator().next();
        Assert.assertNotNull("Injected application instance should not be null", app.app);
    }
}
