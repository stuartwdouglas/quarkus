package io.quarkus.rest.test.security;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Hashtable;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

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
import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestClientBuilder;
import io.quarkus.rest.test.security.resource.BasicAuthBaseResource;
import io.quarkus.rest.test.security.resource.CustomForbiddenMessageExceptionMapper;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Security
 * @tpChapter Integration tests
 * @tpTestCaseDetails Custom ExceptionMapper is used when Forbidden is thrown from RoleBasedSecurityFilter
 * @tpSince RESTEasy 3.1.0
 */
@ServerSetup({ CustomForbiddenMessageTest.SecurityDomainSetup.class })
@Category({ ExpectedFailingOnWildFly18.class }) //WFLY-12655
public class CustomForbiddenMessageTest {

    private static QuarkusRestClient authorizedClient;

    private static final String ACCESS_FORBIDDEN_MESSAGE = "My custom message from CustomForbiddenMessageExceptionMapper: Access forbidden: role not allowed";

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    Hashtable<String, String> contextParams = new Hashtable<String, String>();
                    contextParams.put("resteasy.role.based.security", "true");

                    war.addAsWebInfResource(BasicAuthTest.class.getPackage(), "jboss-web.xml", "/jboss-web.xml")
                            .addAsWebInfResource(BasicAuthTest.class.getPackage(), "web.xml", "/web.xml");

                    return TestUtil.finishContainerPrepare(war, contextParams, BasicAuthBaseResource.class,
                            CustomForbiddenMessageExceptionMapper.class);
                }
            });

    @BeforeClass
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

    @AfterClass
    public static void after() throws Exception {
        authorizedClient.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, CustomForbiddenMessageTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Tests custom message from custom ExceptionMapper
     * @tpSince RESTEasy 3.1.0
     */
    @Test
    public void testCustomExceptionMapper() throws Exception {
        Response response = authorizedClient.target(generateURL("/secured/deny")).request().get();
        Assert.assertEquals(Status.FORBIDDEN, response.getStatus());
        Assert.assertEquals(ACCESS_FORBIDDEN_MESSAGE, response.readEntity(String.class));
        String ct = response.getHeaderString("Content-Type");
        Assert.assertEquals("text/plain;charset=UTF-8", ct);
    }

    static class SecurityDomainSetup extends AbstractUsersRolesSecurityDomainSetup {

        @Override
        public void setConfigurationPath() throws URISyntaxException {
            Path filepath = Paths.get(CustomForbiddenMessageTest.class.getResource("users.properties").toURI());
            Path parent = filepath.getParent();
            createPropertiesFiles(new File(parent.toUri()));
        }
    }
}
