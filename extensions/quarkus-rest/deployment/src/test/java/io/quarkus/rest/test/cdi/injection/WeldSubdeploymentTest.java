package io.quarkus.rest.test.cdi.injection;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.logging.Logger;
import org.jboss.resteasy.utils.TestApplication;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.quarkus.rest.test.cdi.injection.resource.WeldSubdeploymentApplicationResource;
import io.quarkus.rest.test.cdi.injection.resource.WeldSubdeploymentCdiJpaInjectingBean;
import io.quarkus.rest.test.cdi.injection.resource.WeldSubdeploymentRequestResource;
import io.quarkus.rest.test.cdi.injection.resource.WeldSubdeploymentStatefulResource;
import io.quarkus.rest.test.cdi.injection.resource.WeldSubdeploymentStatelessResource;
import io.quarkus.rest.test.simple.PortProviderUtil;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for JBEAP-4084, WFLY-6485
 *                    Test that JPA dependencies are set for sub-deployments
 * @tpSince RESTEasy 3.0.16
 */
public class WeldSubdeploymentTest {

    protected static final Logger logger = Logger.getLogger(WeldSubdeploymentTest.class.getName());

    private static final String WAR_DEPLOYMENT_NAME = "simple";

    public static final String ERROR_MESSAGE = "JBEAP-4084 regression, injected EntityManagerFactory should not be null but is";

    Client client;

    @Deployment
    public static Archive<?> deploy() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "WeldSubdeploymentTest.ear");
        WebArchive war = ShrinkWrap.create(WebArchive.class, String.format("%s.war", WAR_DEPLOYMENT_NAME));
        war.addClasses(WeldSubdeploymentTest.class, WeldSubdeploymentCdiJpaInjectingBean.class, TestApplication.class);
        war.addClasses(WeldSubdeploymentRequestResource.class, WeldSubdeploymentApplicationResource.class,
                WeldSubdeploymentStatefulResource.class, WeldSubdeploymentStatelessResource.class);

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "util.jar");
        jar.addAsManifestResource(WeldSubdeploymentTest.class.getPackage(), "persistence_subdeployment.xml", "persistence.xml");
        war.addAsLibrary(jar);
        ear.addAsModule(war);
        return ear;
    }

    @Before
    public void init() {
        client = ClientBuilder.newClient();
    }

    @After
    public void close() {
        client.close();
    }

    private void testEndPoint(String scope) {
        String url = PortProviderUtil.generateURL(String.format("/%s", scope), WAR_DEPLOYMENT_NAME);
        logger.info(String.format("Request to %s", url));
        WebTarget base = client.target(url);

        Response response = base.request().get();
        assertEquals(Status.NO_CONTENT, response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Test for bean with application scope
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testAppcliationScope() throws Exception {
        testEndPoint("application");
    }

    /**
     * @tpTestDetails Test for bean with request scope
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testRequestScope() throws Exception {
        testEndPoint("request");
    }

    /**
     * @tpTestDetails Test for stateful bean
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testStateful() throws Exception {
        testEndPoint("stateful");
    }

    /**
     * @tpTestDetails Test for stateless bean
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testStateLess() throws Exception {
        testEndPoint("stateless");
    }
}
