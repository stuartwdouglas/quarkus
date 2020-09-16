package io.quarkus.rest.test.injection;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.api.validation.Validation;
import org.jboss.resteasy.api.validation.ViolationReport;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.injection.resource.PostConstructInjectionEJBInterceptor;
import io.quarkus.rest.test.injection.resource.PostConstructInjectionEJBResource;
import io.quarkus.rest.test.injection.resource.PostConstructInjectionResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Validation and @PostConstruct methods
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression tests for RESTEASY-2227
 * @tpSince RESTEasy 3.6
 */
@DisplayName("Post Construct Injection Test")
public class PostConstructInjectionTest {

    static Client client;

    // deployment names
    private static final String WAR_CDI_ON = "war_with_cdi_on";

    private static final String WAR_CDI_OFF = "war_with_cdi_off";

    /**
     * Deployment with CDI activated
     */
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(PostConstructInjectionEJBInterceptor.class);
            // war.addAsWebInfResource(PostConstructInjectionTest.class.getPackage(),
            // "PostConstructInjection_beans_cdi_on.xml", "beans.xml");
            return TestUtil.finishContainerPrepare(war, null, PostConstructInjectionResource.class,
                    PostConstructInjectionEJBResource.class);
        }
    });

    /**
     * Deployment with CDI not activated
     */
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(PostConstructInjectionEJBInterceptor.class);
            // war.addAsWebInfResource(PostConstructInjectionTest.class.getPackage(),
            // "PostConstructInjection_beans_cdi_off.xml", "beans.xml");
            return TestUtil.finishContainerPrepare(war, null, PostConstructInjectionResource.class);
        }
    });

    @BeforeAll
    public static void init() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void after() {
        client.close();
    }

    private String generateURL(String jar, String path) {
        return PortProviderUtil.generateURL(path, PostConstructInjectionTest.class.getSimpleName() + "_CDI_" + jar);
    }

    /**
     * @tpTestDetails In an environment with managed beans, a @PostConstruct method on either an ordinary
     *                resource or an EJB interceptor should execute before class and property validation is done.
     * @tpSince RESTEasy 3.7.0
     */
    @Test
    @DisplayName("Test Post Inject Cdi On")
    public void TestPostInjectCdiOn() throws Exception {
        doTestPostInjectCdiOn("ON", "/normal");
    }

    /**
     * @tpTestDetails In an environment with managed beans, a @PostConstruct method on either an ordinary
     *                resource or an EJB interceptor should execute before class and property validation is done.
     * @tpSince RESTEasy 3.7.0
     */
    @Test
    @Disabled("This test doesn't work yet. See RESTEASY-2264")
    @DisplayName("Test Post Inject Cdi On EJB")
    public void TestPostInjectCdiOnEJB() throws Exception {
        doTestPostInjectCdiOn("ON", "/ejb");
    }

    /**
     * @tpTestDetails In an environment without managed beans, a @PostConstruct method on a resource will not be called.
     * @tpSince RESTEasy 3.7.0
     */
    @Test
    @DisplayName("Test Post Inject Cdi Off")
    public void TestPostInjectCdiOff() throws Exception {
        Response response = client.target(generateURL("OFF", "/normal/get")).request().get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "ab");
        response.close();
    }

    void doTestPostInjectCdiOn(String cdi, String resource) {
        Response response = client.target(generateURL(cdi, resource + "/get")).request().get();
        Assertions.assertEquals(400, response.getStatus());
        String header = response.getHeaderString(Validation.VALIDATION_HEADER);
        Assertions.assertNotNull(header);
        ViolationReport report = response.readEntity(ViolationReport.class);
        Assertions.assertEquals(1, report.getPropertyViolations().size());
        response.close();
    }
}
