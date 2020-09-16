package io.quarkus.rest.test.providers.custom;

import static io.quarkus.rest.test.ContainerConstants.DEFAULT_CONTAINER_QUALIFIER;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Feature;
import javax.ws.rs.ext.ReaderInterceptor;


import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.ContainerConstants;
import io.quarkus.rest.test.providers.custom.resource.DuplicateProviderRegistrationFeature;
import io.quarkus.rest.test.providers.custom.resource.DuplicateProviderRegistrationFilter;
import io.quarkus.rest.test.providers.custom.resource.DuplicateProviderRegistrationInterceptor;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Providers
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for JBEAP-4703
 * @tpSince RESTEasy 3.0.17
 */
@DisplayName("Duplicate Provider Registration Test")
public class DuplicateProviderRegistrationTest {

    private static final String RESTEASY_002155_ERR_MSG = "Wrong count of RESTEASY002155 warning message";

    private static final String RESTEASY_002160_ERR_MSG = "Wrong count of RESTEASY002160 warning message";

    @SuppressWarnings(value = "unchecked")
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(DuplicateProviderRegistrationFeature.class, DuplicateProviderRegistrationFilter.class,
                    TestUtil.class, DuplicateProviderRegistrationInterceptor.class, ContainerConstants.class);
            // Arquillian in the deployment, test reads the server.log
            return TestUtil.finishContainerPrepare(war, null, (Class<?>[]) null);
        }
    });

    private static int getRESTEASY002155WarningCount() {
        return TestUtil.getWarningCount("RESTEASY002155", true, DEFAULT_CONTAINER_QUALIFIER);
    }

    private static int getRESTEASY002160WarningCount() {
        return TestUtil.getWarningCount("RESTEASY002160", true, DEFAULT_CONTAINER_QUALIFIER);
    }

    /**
     * @tpTestDetails Basic test
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    @DisplayName("Test Duplicate Provider")
    public void testDuplicateProvider() {
        int initRESTEASY002160WarningCount = getRESTEASY002160WarningCount();
        Client client = ClientBuilder.newClient();
        try {
            WebTarget webTarget = client.target("http://www.changeit.com");
            // DuplicateProviderRegistrationFeature will be registered third on the same webTarget even if
            // webTarget.getConfiguration().isRegistered(DuplicateProviderRegistrationFeature.class)==true
            webTarget.register(DuplicateProviderRegistrationFeature.class).register(new DuplicateProviderRegistrationFeature())
                    .register(new DuplicateProviderRegistrationFeature());
        } finally {
            client.close();
        }
        Assertions.assertEquals(RESTEASY_002160_ERR_MSG, 2, getRESTEASY002160WarningCount() - initRESTEASY002160WarningCount);
    }

    /**
     * @tpTestDetails This test is taken from javax.ws.rs.core.Configurable javadoc
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    @DisplayName("Test From Javadoc")
    public void testFromJavadoc() {
        int initRESTEASY002155WarningCount = getRESTEASY002155WarningCount();
        int initRESTEASY002160WarningCount = getRESTEASY002160WarningCount();
        Client client = ClientBuilder.newClient();
        try {
            WebTarget webTarget = client.target("http://www.changeit.com");
            webTarget.register(DuplicateProviderRegistrationInterceptor.class, ReaderInterceptor.class);
            // Rejected by runtime.
            webTarget.register(DuplicateProviderRegistrationInterceptor.class);
            // Rejected by runtime.
            webTarget.register(new DuplicateProviderRegistrationInterceptor());
            // Rejected by runtime.
            webTarget.register(DuplicateProviderRegistrationInterceptor.class, 6500);
            webTarget.register(new DuplicateProviderRegistrationFeature());
            // rejected by runtime.
            webTarget.register(new DuplicateProviderRegistrationFeature());
            // rejected by runtime.
            webTarget.register(DuplicateProviderRegistrationFeature.class);
            // Rejected by runtime.
            webTarget.register(DuplicateProviderRegistrationFeature.class, Feature.class);
        } finally {
            client.close();
        }
        Assertions.assertEquals(RESTEASY_002155_ERR_MSG, 4, getRESTEASY002155WarningCount() - initRESTEASY002155WarningCount);
        Assertions.assertEquals(RESTEASY_002160_ERR_MSG, 2, getRESTEASY002160WarningCount() - initRESTEASY002160WarningCount);
    }
}
