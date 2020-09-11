package io.quarkus.rest.test.warning;

import static io.quarkus.rest.test.ContainerConstants.DEFAULT_CONTAINER_QUALIFIER;

import java.util.function.Supplier;

import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

import io.quarkus.rest.test.core.interceptors.resource.TestResource1;
import io.quarkus.rest.test.core.interceptors.resource.TestResource2;
import io.quarkus.rest.test.core.interceptors.resource.TestSubResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.rest.test.warning.resource.SubResourceWarningResource;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Miscellaneous
 * @tpChapter Integration tests
 * @tpSince RESTEasy 4.0.0
 *          Created by rsearls on 9/11/17.
 */
public class SubResourceWarningTest {

    // check server.log msg count before app is deployed.  Deploying causes messages to be logged.
    private static int preTestCnt = TestUtil.getWarningCount("have the same path, [test", false, DEFAULT_CONTAINER_QUALIFIER);

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, SubResourceWarningResource.class,
                            TestResource1.class, TestResource2.class, TestSubResource.class);
                }
            });

    @BeforeClass
    public static void initLogging() throws Exception {
        OnlineManagementClient client = TestUtil.clientInit();
        TestUtil.runCmd(client, "/subsystem=logging/logger=org.jboss.resteasy:add(level=WARN)");
        client.close();
    }

    @AfterClass
    public static void removeLogging() throws Exception {
        OnlineManagementClient client = TestUtil.clientInit();
        TestUtil.runCmd(client, "/subsystem=logging/logger=org.jboss.resteasy:remove()");
        client.close();
    }

    /**
     * Confirms that 2 warning messages about this incorrect coding is printed to the server.log
     * Must check for path because warning text, RESTEASY002195, exist in log for a previous test
     * in the suite.
     * 
     * @throws Exception
     */
    @Test
    public void testWarningMsg() throws Exception {
        int cnt = TestUtil.getWarningCount("have the same path, [test", false, DEFAULT_CONTAINER_QUALIFIER);
        Assert.assertEquals("Improper log WARNING count", preTestCnt + 2, cnt);
    }
}
