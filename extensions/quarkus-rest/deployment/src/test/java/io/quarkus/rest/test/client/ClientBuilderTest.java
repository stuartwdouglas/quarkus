package io.quarkus.rest.test.client;

import static io.quarkus.rest.test.ContainerConstants.DEFAULT_CONTAINER_QUALIFIER;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Unit tests
 * @tpSince RESTEasy 3.0.17
 */

public class ClientBuilderTest {

    @SuppressWarnings(value = "unchecked")
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(TestUtil.class);
                    // Arquillian in the deployment and use of TestUtil

                    return TestUtil.finishContainerPrepare(war, null, (Class<?>[]) null);
                }
            });

    public static class FeatureReturningFalse implements Feature {
        @Override
        public boolean configure(FeatureContext context) {
            // false returning feature is not to be registered
            return false;
        }
    }

    private int getWarningCount() {
        return TestUtil.getWarningCount("RESTEASY002155", true, DEFAULT_CONTAINER_QUALIFIER);
    }

    /**
     * @tpTestDetails Register class twice to the client
     * @tpPassCrit Warning will be raised that second class registration is ignored
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    public void testDoubleClassRegistration() {
        int initCount = getWarningCount();
        Client client = ClientBuilder.newClient();
        int count = client.getConfiguration().getClasses().size();
        client.register(FeatureReturningFalse.class).register(FeatureReturningFalse.class);

        Assert.assertEquals("RESTEASY002155 log not found", 1, getWarningCount() - initCount);
        Assert.assertEquals(count + 1, client.getConfiguration().getClasses().size());
        client.close();
    }

    /**
     * @tpTestDetails Register provider instance twice to the client
     * @tpPassCrit Warning will be raised that second provider instance registration is ignored
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    public void testDoubleRegistration() {
        int countRESTEASY002160 = TestUtil.getWarningCount("RESTEASY002160", true, DEFAULT_CONTAINER_QUALIFIER);
        int countRESTEASY002155 = TestUtil.getWarningCount("RESTEASY002155", true, DEFAULT_CONTAINER_QUALIFIER);
        Client client = ClientBuilder.newClient();
        int count = client.getConfiguration().getInstances().size();
        Object reg = new FeatureReturningFalse();

        client.register(reg).register(reg);
        client.register(FeatureReturningFalse.class).register(FeatureReturningFalse.class);
        Assert.assertEquals("Expect 1 warnining messages of Provider instance is already registered", 1,
                TestUtil.getWarningCount("RESTEASY002160", true, DEFAULT_CONTAINER_QUALIFIER) - countRESTEASY002160);
        Assert.assertEquals("Expect 1 warnining messages of Provider class is already registered", 2,
                TestUtil.getWarningCount("RESTEASY002155", true, DEFAULT_CONTAINER_QUALIFIER) - countRESTEASY002155);
        Assert.assertEquals(count + 1, client.getConfiguration().getInstances().size());

        client.close();
    }
}
