package io.quarkus.rest.test.security;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Hashtable;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.resteasy.category.ExpectedFailingOnWildFly18;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClientEngine;
import org.jboss.resteasy.setup.AbstractUsersRolesSecurityDomainSetup;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestClientBuilder;
import io.quarkus.rest.test.security.resource.BasicAuthBaseResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Security
 * @tpChapter Integration tests
 * @tpTestCaseDetails Two different security domains in two deployments. Both domains are by default created in PicketBox
 *                    security subsystem. When running server and tests Elytron enabled, domain in the deployment 2 is created
 *                    in the Elytron subsystem.
 * @tpSince RESTEasy 3.0.21
 */
@ServerSetup({ TwoSecurityDomainsTest.SecurityDomainSetup1.class, TwoSecurityDomainsTest.SecurityDomainSetup2.class })
// WFLY-12655
@Category({ ExpectedFailingOnWildFly18.class })
@DisplayName("Two Security Domains Test")
public class TwoSecurityDomainsTest {

    private static QuarkusRestClient authorizedClient;

    private static final String SECURITY_DOMAIN_DEPLOYMENT_1 = "jaxrsSecDomain";

    private static final String SECURITY_DOMAIN_DEPLOYMENT_2 = "jaxrsSecDomain2";

    private static final String WRONG_RESPONSE = "Wrong response content.";

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            Hashtable<String, String> contextParams = new Hashtable<String, String>();
            contextParams.put("resteasy.role.based.security", "true");
            // war.addAsWebInfResource(BasicAuthTest.class.getPackage(), "jboss-web.xml", "/jboss-web.xml")
            // .addAsWebInfResource(TwoSecurityDomainsTest.class.getPackage(), "web.xml", "/web.xml");
            return TestUtil.finishContainerPrepare(war, contextParams, BasicAuthBaseResource.class);
        }
    });

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            Hashtable<String, String> contextParams = new Hashtable<String, String>();
            contextParams.put("resteasy.role.based.security", "true");
            // war.addAsWebInfResource(BasicAuthTest.class.getPackage(), "jboss-web2.xml", "/jboss-web.xml")
            // .addAsWebInfResource(TwoSecurityDomainsTest.class.getPackage(), "web.xml", "/web.xml");
            return TestUtil.finishContainerPrepare(war, contextParams, BasicAuthBaseResource.class);
        }
    });

    @BeforeAll
    public static void init() {
        // authorizedClient
        {
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("bill", "password1");
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(new AuthScope(AuthScope.ANY), credentials);
            CloseableHttpClient client = HttpClients.custom().setDefaultCredentialsProvider(credentialsProvider).build();
            ApacheHttpClientEngine engine = ApacheHttpClientEngine.create(client);
            authorizedClient = ((QuarkusRestClientBuilder) ClientBuilder.newBuilder()).httpEngine(engine).build();
        }
    }

    @AfterAll
    public static void after() throws Exception {
        authorizedClient.close();
    }

    /**
     * @tpTestDetails Client using correct authorization credentials sends GET request to the first and then second deployment
     * @tpSince RESTEasy 3.0.21
     */
    @Test
    @DisplayName("Test One Client Two Deployments Two Security Domains")
    public void testOneClientTwoDeploymentsTwoSecurityDomains() throws Exception {
        Response response = authorizedClient.target(PortProviderUtil.generateURL("/secured",
                TwoSecurityDomainsTest.class.getSimpleName() + SECURITY_DOMAIN_DEPLOYMENT_1)).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(WRONG_RESPONSE, "hello", response.readEntity(String.class));
        response = authorizedClient.target(PortProviderUtil.generateURL("/secured",
                TwoSecurityDomainsTest.class.getSimpleName() + SECURITY_DOMAIN_DEPLOYMENT_2)).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(WRONG_RESPONSE, "hello", response.readEntity(String.class));
    }

    @DisplayName("Security Domain Setup 1")
    static class SecurityDomainSetup1 extends AbstractUsersRolesSecurityDomainSetup {

        @Override
        public void setConfigurationPath() throws URISyntaxException {
            Path filepath = Paths.get(TwoSecurityDomainsTest.class.getResource("users.properties").toURI());
            Path parent = filepath.getParent();
            createPropertiesFiles(new File(parent.toUri()));
            setSecurityDomainName(SECURITY_DOMAIN_DEPLOYMENT_1);
            setSubsystem("picketBox");
        }
    }

    @DisplayName("Security Domain Setup 2")
    static class SecurityDomainSetup2 extends AbstractUsersRolesSecurityDomainSetup {

        @Override
        public void setConfigurationPath() throws URISyntaxException {
            Path filepath = Paths.get(TwoSecurityDomainsTest.class.getResource("users.properties").toURI());
            Path parent = filepath.getParent();
            createPropertiesFiles(new File(parent.toUri()));
            setSecurityDomainName(SECURITY_DOMAIN_DEPLOYMENT_2);
        }
    }
}
