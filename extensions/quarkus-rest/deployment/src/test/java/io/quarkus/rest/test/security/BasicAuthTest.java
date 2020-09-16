package io.quarkus.rest.test.security;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Hashtable;
import java.util.function.Supplier;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClientEngine;
import org.jboss.resteasy.setup.AbstractUsersRolesSecurityDomainSetup;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestClientBuilder;
import io.quarkus.rest.test.security.resource.BasicAuthBaseProxy;
import io.quarkus.rest.test.security.resource.BasicAuthBaseResource;
import io.quarkus.rest.test.security.resource.BasicAuthBaseResourceAnybody;
import io.quarkus.rest.test.security.resource.BasicAuthBaseResourceMoreSecured;
import io.quarkus.rest.test.security.resource.BasicAuthRequestFilter;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Security
 * @tpChapter Integration tests
 * @tpTestCaseDetails Basic test for RESTEasy authentication.
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Basic Auth Test")
public class BasicAuthTest {

    private static final String WRONG_RESPONSE = "Wrong response content.";

    private static final String ACCESS_FORBIDDEN_MESSAGE = "Access forbidden: role not allowed";

    private static QuarkusRestClient authorizedClient;

    private static QuarkusRestClient unauthorizedClient;

    private static QuarkusRestClient noAutorizationClient;

    // Following clients are used in tests for ClientRequestFilter
    private static QuarkusRestClient authorizedClientUsingRequestFilter;

    private static QuarkusRestClient unauthorizedClientUsingRequestFilter;

    private static QuarkusRestClient unauthorizedClientUsingRequestFilterWithWrongPassword;

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
        // unauthorizedClient
        {
            UsernamePasswordCredentials credentials_other = new UsernamePasswordCredentials("ordinaryUser", "password2");
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(new AuthScope(AuthScope.ANY), credentials_other);
            CloseableHttpClient client = HttpClients.custom().setDefaultCredentialsProvider(credentialsProvider).build();
            ApacheHttpClientEngine engine = ApacheHttpClientEngine.create(client);
            unauthorizedClient = ((QuarkusRestClientBuilder) ClientBuilder.newBuilder()).httpEngine(engine).build();
        }
        // noAuthorizationClient
        noAutorizationClient = (QuarkusRestClient) ClientBuilder.newClient();
        // authorizedClient with ClientRequestFilter
        {
            QuarkusRestClientBuilder builder = (QuarkusRestClientBuilder) ClientBuilder.newBuilder();
            authorizedClientUsingRequestFilter = (QuarkusRestClient) builder
                    .register(new BasicAuthRequestFilter("bill", "password1")).build();
        }
        // unauthorizedClient with ClientRequestFilter - unauthorized user
        {
            QuarkusRestClientBuilder builder = (QuarkusRestClientBuilder) ClientBuilder.newBuilder();
            unauthorizedClientUsingRequestFilter = (QuarkusRestClient) builder
                    .register(new BasicAuthRequestFilter("ordinaryUser", "password2")).build();
        }
        // unauthorizedClient with ClientRequestFilter - wrong password
        {
            QuarkusRestClientBuilder builder = (QuarkusRestClientBuilder) ClientBuilder.newBuilder();
            unauthorizedClientUsingRequestFilterWithWrongPassword = (QuarkusRestClient) builder
                    .register(new BasicAuthRequestFilter("bill", "password2")).build();
        }
    }

    @AfterAll
    public static void after() throws Exception {
        authorizedClient.close();
        unauthorizedClient.close();
        noAutorizationClient.close();
        authorizedClientUsingRequestFilter.close();
        unauthorizedClientUsingRequestFilter.close();
        unauthorizedClientUsingRequestFilterWithWrongPassword.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            Hashtable<String, String> contextParams = new Hashtable<String, String>();
            contextParams.put("resteasy.role.based.security", "true");
            war.addClass(BasicAuthBaseProxy.class);
            // .addAsWebInfResource(BasicAuthTest.class.getPackage(), "jboss-web.xml", "/jboss-web.xml")
            // .addAsWebInfResource(BasicAuthTest.class.getPackage(), "web.xml", "/web.xml");
            return TestUtil.finishContainerPrepare(war, contextParams, BasicAuthBaseResource.class,
                    BasicAuthBaseResourceMoreSecured.class, BasicAuthBaseResourceAnybody.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, BasicAuthTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Basic ProxyFactory test. Correct credentials are used.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Proxy")
    public void testProxy() throws Exception {
        BasicAuthBaseProxy proxy = authorizedClient.target(generateURL("/")).proxyBuilder(BasicAuthBaseProxy.class).build();
        Assertions.assertEquals(WRONG_RESPONSE, proxy.get(), "hello");
        Assertions.assertEquals(WRONG_RESPONSE, proxy.getAuthorized(), "authorized");
    }

    /**
     * @tpTestDetails Basic ProxyFactory test. No credentials are used.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Proxy Failure")
    public void testProxyFailure() throws Exception {
        BasicAuthBaseProxy proxy = noAutorizationClient.target(generateURL("/")).proxyBuilder(BasicAuthBaseProxy.class).build();
        try {
            proxy.getFailure();
            Assert.fail();
        } catch (NotAuthorizedException e) {
            Assertions.assertEquals(Status.UNAUTHORIZED.getStatusCode(), e.getResponse().getStatus());
            Assertions.assertTrue(e.getResponse().getHeaderString("WWW-Authenticate").contains("Basic realm="),
                    "WWW-Authenticate header is not included");
        }
    }

    /**
     * @tpTestDetails Test secured resource with correct and incorrect credentials.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Security")
    public void testSecurity() throws Exception {
        // authorized client
        {
            Response response = authorizedClient.target(generateURL("/secured")).request().get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals(WRONG_RESPONSE, "hello", response.readEntity(String.class));
        }
        {
            Response response = authorizedClient.target(generateURL("/secured/authorized")).request().get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals(WRONG_RESPONSE, "authorized", response.readEntity(String.class));
        }
        {
            Response response = authorizedClient.target(generateURL("/secured/deny")).request().get();
            Assertions.assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
            Assertions.assertEquals(WRONG_RESPONSE, ACCESS_FORBIDDEN_MESSAGE, response.readEntity(String.class));
        }
        {
            Response response = authorizedClient.target(generateURL("/secured3/authorized")).request().get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals(WRONG_RESPONSE, "authorized", response.readEntity(String.class));
        }
        // unauthorized client
        {
            Response response = unauthorizedClient.target(generateURL("/secured3/authorized")).request().get();
            Assertions.assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
            Assertions.assertEquals(WRONG_RESPONSE, ACCESS_FORBIDDEN_MESSAGE, response.readEntity(String.class));
        }
        {
            Response response = unauthorizedClient.target(generateURL("/secured3/anybody")).request().get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            response.close();
        }
    }

    /**
     * @tpTestDetails Regression test for RESTEASY-579
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test 579")
    public void test579() throws Exception {
        Response response = authorizedClient.target(generateURL("/secured2")).request().get();
        Assertions.assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Check failures for secured resource.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Security Failure")
    public void testSecurityFailure() throws Exception {
        {
            Response response = noAutorizationClient.target(generateURL("/secured")).request().get();
            Assertions.assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
            Assertions.assertTrue(response.getHeaderString("WWW-Authenticate").contains("Basic realm="),
                    "WWW-Authenticate header is not included");
            response.close();
        }
        {
            Response response = authorizedClient.target(generateURL("/secured/authorized")).request().get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals(WRONG_RESPONSE, "authorized", response.readEntity(String.class));
        }
        {
            Response response = unauthorizedClient.target(generateURL("/secured/authorized")).request().get();
            Assertions.assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
            Assertions.assertEquals(ACCESS_FORBIDDEN_MESSAGE, response.readEntity(String.class));
        }
    }

    /**
     * @tpTestDetails Regression test for JBEAP-1589, RESTEASY-1249
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Acces Forbidden Message")
    public void testAccesForbiddenMessage() throws Exception {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("bill", "password1");
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(new AuthScope(AuthScope.ANY), credentials);
        CloseableHttpClient client = HttpClients.custom().setDefaultCredentialsProvider(credentialsProvider).build();
        ApacheHttpClientEngine engine = ApacheHttpClientEngine.create(client);
        QuarkusRestClient authorizedClient = ((QuarkusRestClientBuilder) ClientBuilder.newBuilder()).httpEngine(engine).build();
        Response response = authorizedClient.target(generateURL("/secured/deny")).request().get();
        Assertions.assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
        Assertions.assertEquals(ACCESS_FORBIDDEN_MESSAGE, response.readEntity(String.class));
        authorizedClient.close();
    }

    /**
     * @tpTestDetails Test Content-type when forbidden exception is raised, RESTEASY-1563
     * @tpSince RESTEasy 3.1.1
     */
    @Test
    @DisplayName("Test Content Type With Forbidden Message")
    public void testContentTypeWithForbiddenMessage() {
        Response response = unauthorizedClient.target(generateURL("/secured/denyWithContentType")).request().get();
        Assertions.assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.getHeaderString("Content-type"), "Incorrect Content-type header",
                "text/html;charset=UTF-8");
        Assertions.assertEquals(ACCESS_FORBIDDEN_MESSAGE, response.readEntity(String.class),
                "Missing forbidden message in the response");
    }

    /**
     * @tpTestDetails Test Content-type when unauthorized exception is raised
     * @tpSince RESTEasy 3.1.1
     */
    @Test
    @DisplayName("Test Content Type With Unauthorized Message")
    public void testContentTypeWithUnauthorizedMessage() {
        Response response = noAutorizationClient.target(generateURL("/secured/denyWithContentType")).request().get();
        Assertions.assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.getHeaderString("Content-type"), "Incorrect Content-type header",
                "text/html;charset=UTF-8");
        Assertions.assertTrue(response.getHeaderString("WWW-Authenticate").contains("Basic realm="),
                "WWW-Authenticate header is not included");
    }

    /**
     * @tpTestDetails Test secured resource with correct credentials. Authentication is done using BasicAuthRequestFilter.
     * @tpSince RESTEasy 3.7.0
     */
    @Test
    @DisplayName("Test With Client Request Filter Authorized User")
    public void testWithClientRequestFilterAuthorizedUser() {
        Response response = authorizedClientUsingRequestFilter.target(generateURL("/secured/authorized")).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(WRONG_RESPONSE, "authorized", response.readEntity(String.class));
    }

    /**
     * @tpTestDetails Test secured resource with incorrect credentials. Authentication is done using BasicAuthRequestFilter.
     * @tpSince RESTEasy 3.7.0
     */
    @Test
    @DisplayName("Test With Client Request Filter Wrong Password")
    public void testWithClientRequestFilterWrongPassword() {
        Response response = unauthorizedClientUsingRequestFilterWithWrongPassword.target(generateURL("/secured/authorized"))
                .request().get();
        Assertions.assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        Assertions.assertTrue(response.getHeaderString("WWW-Authenticate").contains("Basic realm="),
                "WWW-Authenticate header is not included");
    }

    /**
     * @tpTestDetails Test secured resource with correct credentials of user that is not authorized to the resource.
     *                Authentication is done using BasicAuthRequestFilter.
     * @tpSince RESTEasy 3.7.0
     */
    @Test
    @DisplayName("Test With Client Request Filter Unauthorized User")
    public void testWithClientRequestFilterUnauthorizedUser() {
        Response response = unauthorizedClientUsingRequestFilter.target(generateURL("/secured/authorized")).request().get();
        Assertions.assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
        Assertions.assertEquals(WRONG_RESPONSE, ACCESS_FORBIDDEN_MESSAGE, response.readEntity(String.class));
    }

    /**
     * @tpTestDetails Test that client correctly loads ClientConfigProvider implementation and uses credentials when making a
     *                request.
     *                Also test these credentials are ignored if different are set.
     */
    @Test
    @DisplayName("Test Client Config Provider Credentials")
    public void testClientConfigProviderCredentials() throws IOException {
        String jarPath = ClientConfigProviderTestJarHelper.createClientConfigProviderTestJarWithBASIC();
        Process process = ClientConfigProviderTestJarHelper.runClientConfigProviderTestJar(
                ClientConfigProviderTestJarHelper.TestType.TEST_CREDENTIALS_ARE_USED_FOR_BASIC, jarPath,
                new String[] { generateURL("/secured/authorized") });
        String line = ClientConfigProviderTestJarHelper.getResultOfProcess(process);
        Assertions.assertEquals(line, "200");
        process.destroy();
        process = ClientConfigProviderTestJarHelper.runClientConfigProviderTestJar(
                ClientConfigProviderTestJarHelper.TestType.TEST_CLIENTCONFIG_CREDENTIALS_ARE_IGNORED_IF_DIFFERENT_SET, jarPath,
                new String[] { generateURL("/secured/authorized") });
        line = ClientConfigProviderTestJarHelper.getResultOfProcess(process);
        Assertions.assertEquals(line, "401");
        process.destroy();
        Assertions.assertTrue(new File(jarPath).delete());
    }

    @DisplayName("Security Domain Setup")
    static class SecurityDomainSetup extends AbstractUsersRolesSecurityDomainSetup {

        @Override
        public void setConfigurationPath() throws URISyntaxException {
            Path filepath = Paths.get(BasicAuthTest.class.getResource("users.properties").toURI());
            Path parent = filepath.getParent();
            createPropertiesFiles(new File(parent.toUri()));
        }
    }
}
