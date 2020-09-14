package io.quarkus.rest.test.security;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.resteasy.client.jaxrs.internal.BasicAuthentication;
import org.jboss.resteasy.setup.AbstractUsersRolesSecurityDomainSetup;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.wildfly.extras.creaper.core.CommandFailedException;

import io.quarkus.rest.test.security.resource.SecurityContextContainerRequestFilter;
import io.quarkus.rest.test.security.resource.SecurityContextResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Security
 * @tpChapter Integration tests
 * @tpTestCaseDetails Basic test for RESTEasy authentication using programmatic security with javax.ws.rs.core.SecurityContext
 * @tpSince RESTEasy 3.0.16
 */
@ServerSetup({ SecurityContextTest.SecurityDomainSetup.class })
public class SecurityContextTest {

    private static final String USERNAME = "bill";
    private static final String PASSWORD = "password1";

    private static final String USERNAME2 = "ordinaryUser";
    private static final String PASSWORD2 = "password2";

    private Client authorizedClient;
    private Client nonauthorizedClient;

    @Before
    public void initClient() throws IOException, CommandFailedException {

        // Create jaxrs client
        nonauthorizedClient = ClientBuilder.newClient();
        nonauthorizedClient.register(new BasicAuthentication(USERNAME2, PASSWORD2));

        // Create jaxrs client
        authorizedClient = ClientBuilder.newClient();
        authorizedClient.register(new BasicAuthentication(USERNAME, PASSWORD));

    }

    @After
    public void after() throws Exception {
        authorizedClient.close();
        nonauthorizedClient.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addAsWebInfResource(SecurityContextTest.class.getPackage(), "jboss-web.xml", "jboss-web.xml")
                            .addAsWebInfResource(SecurityContextTest.class.getPackage(), "securityContext/web.xml", "web.xml");
                    return TestUtil.finishContainerPrepare(war, null, SecurityContextResource.class);
                }
            });

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addAsWebInfResource(SecurityContextTest.class.getPackage(), "jboss-web.xml", "jboss-web.xml")
                            .addAsWebInfResource(SecurityContextTest.class.getPackage(), "securityContext/web.xml", "web.xml");
                    return TestUtil.finishContainerPrepare(war, null, SecurityContextResource.class,
                            SecurityContextContainerRequestFilter.class);
                }
            });

    /**
     * @tpTestDetails Correct credentials are used.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testSecurityContextAuthorized() {
        Response response = authorizedClient
                .target(PortProviderUtil.generateURL("/test", SecurityContextTest.class.getSimpleName())).request().get();
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("Good user bill", response.readEntity(String.class));
    }

    /**
     * @tpTestDetails Incorrect credentials are used.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testSecurityContextNonAuthorized() {
        Response response = nonauthorizedClient
                .target(PortProviderUtil.generateURL("/test", SecurityContextTest.class.getSimpleName())).request().get();
        Assert.assertEquals("User ordinaryUser is not authorized", response.readEntity(String.class));
        Assert.assertEquals(Status.UNAUTHORIZED, response.getStatus());
    }

    /**
     * @tpTestDetails ContainerRequestFilter and correct credentials are used
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @OperateOnDeployment("containerRequestFilter")
    public void testSecurityContextAuthorizedUsingFilter() {
        Response response = authorizedClient
                .target(PortProviderUtil.generateURL("/test", SecurityContextTest.class.getSimpleName() + "Filter")).request()
                .get();
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("Good user bill", response.readEntity(String.class));
    }

    /**
     * @tpTestDetails ContainerRequestFilter and incorrect credentials are used.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @OperateOnDeployment("containerRequestFilter")
    public void testSecurityContextNonAuthorizedUsingFilter() {
        Response response = nonauthorizedClient
                .target(PortProviderUtil.generateURL("/test", SecurityContextTest.class.getSimpleName() + "Filter")).request()
                .get();
        Assert.assertEquals("User ordinaryUser is not authorized, coming from filter", response.readEntity(String.class));
        Assert.assertEquals(Status.UNAUTHORIZED, response.getStatus());
    }

    static class SecurityDomainSetup extends AbstractUsersRolesSecurityDomainSetup {

        @Override
        public void setConfigurationPath() throws URISyntaxException {
            Path filepath = Paths.get(SecurityContextTest.class.getResource("users.properties").toURI());
            Path parent = filepath.getParent();
            createPropertiesFiles(new File(parent.toUri()));
        }
    }
}
