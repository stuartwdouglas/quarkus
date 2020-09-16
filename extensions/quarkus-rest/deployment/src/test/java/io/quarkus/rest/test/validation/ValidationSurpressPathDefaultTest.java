package io.quarkus.rest.test.validation;

import java.util.function.Supplier;

import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Response
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for default value of resteasy.validation.suppress.path parameter
 * @tpSince RESTEasy 3.0.16
 */
public class ValidationSurpressPathDefaultTest extends ValidationSuppressPathTestBase {
    @SuppressWarnings(value = "unchecked")
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, (Class<?>[]) null);
                }
            });

    /**
     * @tpTestDetails Test input violations.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testInputViolations() throws Exception {
        doTestInputViolations("s", "t", "", new String[] { "post.arg0", "post.foo" });
    }

    /**
     * @tpTestDetails Test return value violations.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testReturnValues() throws Exception {
        doTestReturnValueViolations("postNative.<return value>");
    }
}
