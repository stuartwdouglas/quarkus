package io.quarkus.rest.test.core.basic;

import static io.quarkus.rest.test.ContainerConstants.DEFAULT_CONTAINER_QUALIFIER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;

import io.quarkus.rest.test.core.basic.resource.DuplicateDeploymentReader;
import io.quarkus.rest.test.core.basic.resource.DuplicateDeploymentResource;
import io.quarkus.rest.test.simple.TestUtil;

/**
 * @tpSubChapter Response
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.17
 * @tpTestCaseDetails Regression test for JBEAP-4697
 */
public class DuplicateDeploymentTest {
    private static int initWarningCount = 0;

    private static int getWarningCount() {
        return TestUtil.getWarningCount("RESTEASY002172", false, DEFAULT_CONTAINER_QUALIFIER);
    }

    @Deployment
    public static Archive<?> deploy() {
        initWarningCount = getWarningCount();
        WebArchive war = TestUtil.prepareArchive(DuplicateDeploymentTest.class.getSimpleName());

        List<Class<?>> singletons = new ArrayList<>();
        singletons.add(DuplicateDeploymentResource.class);
        singletons.add(DuplicateDeploymentReader.class);

        return TestUtil.finishContainerPrepare(war, null, singletons, DuplicateDeploymentResource.class,
                DuplicateDeploymentReader.class);
    }

    /**
     * @tpTestDetails Check number of warning messages: Singleton resource object class "Resource" already deployed. Singleton
     *                ignored.
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    public void testDeploy() throws IOException {
        Assert.assertEquals("Wrong count of warning messages in logs", 2, getWarningCount() - initWarningCount);
    }
}
