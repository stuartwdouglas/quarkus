package io.quarkus.rest.test.providers.custom;

import static io.quarkus.rest.test.ContainerConstants.DEFAULT_CONTAINER_QUALIFIER;

import java.util.function.Supplier;

import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Core
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.17
 * @tpTestCaseDetails Regression test for JBEAP-4719
 */
@DisplayName("Missing Producer Test")
public class MissingProducerTest {

    private static final String ERR_MSG = "Warning was not logged";

    private static int initLogMsg1Count = parseLog1();

    private static int initLogMsg2Count = parseLog2();

    private static int initLogMsg3Count = parseLog3();

    private static int parseLog1() {
        return TestUtil.getWarningCount("RESTEASY002120: ClassNotFoundException: ", false, DEFAULT_CONTAINER_QUALIFIER);
    }

    private static int parseLog2() {
        return TestUtil.getWarningCount("Unable to load builtin provider org.jboss.resteasy.Missing from ", false,
                DEFAULT_CONTAINER_QUALIFIER);
    }

    private static int parseLog3() {
        return TestUtil.getWarningCount("classes/META-INF/services/javax.ws.rs.ext.Providers", false,
                DEFAULT_CONTAINER_QUALIFIER);
    }

    @SuppressWarnings(value = "unchecked")
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addAsResource(MissingProducerTest.class.getPackage(), "MissingProducer.Providers",
                    "META-INF/services/javax.ws.rs.ext.Providers");
            return TestUtil.finishContainerPrepare(war, null, (Class<?>[]) null);
        }
    });

    /**
     * @tpTestDetails Check logs for RESTEASY002120 warning message.
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    @DisplayName("Test Missing Producer")
    public void testMissingProducer() {
        Assertions.assertEquals(ERR_MSG, 1, parseLog1() - initLogMsg1Count);
        Assertions.assertEquals(ERR_MSG, 1, parseLog2() - initLogMsg2Count);
        Assertions.assertEquals(ERR_MSG, 1, parseLog3() - initLogMsg3Count);
    }
}
