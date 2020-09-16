package io.quarkus.rest.test.security;

import static io.quarkus.rest.test.ContainerConstants.SSL_CONTAINER_PORT_OFFSET;
import static io.quarkus.rest.test.ContainerConstants.SSL_CONTAINER_QUALIFIER;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.function.Supplier;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClientBuilder;
import io.quarkus.rest.test.security.resource.CustomTrustManager;
import io.quarkus.rest.test.security.resource.SslResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Security
 * @tpChapter Integration tests
 * @tpTestCaseDetails Tests for SSL - server secured with correct certificate for "localhost"
 * @tpSince RESTEasy 3.7.0
 */
@DisplayName("Ssl Server With Correct Certificate Test")
public class SslServerWithCorrectCertificateTest extends SslTestBase {

    private static final Logger LOG = Logger.getLogger(SslServerWithCorrectCertificateTest.class.getName());

    private static KeyStore correctTruststore;

    private static KeyStore differentTruststore;

    private static final String SERVER_KEYSTORE_PATH = RESOURCES + "/server.keystore";

    private static final String CLIENT_TRUSTSTORE_PATH = RESOURCES + "/client.truststore";

    private static final String DIFFERENT_CLIENT_TRUSTSTORE_PATH = RESOURCES + "/client-different-cert.truststore";

    private static final String URL = generateHttpsURL(SSL_CONTAINER_PORT_OFFSET);

    @TargetsContainer(SSL_CONTAINER_QUALIFIER)
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, SslResource.class);
        }
    });

    @BeforeAll
    public static void prepareTruststores()
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        correctTruststore = KeyStore.getInstance("jks");
        try (InputStream in = new FileInputStream(CLIENT_TRUSTSTORE_PATH)) {
            correctTruststore.load(in, PASSWORD.toCharArray());
        }
        differentTruststore = KeyStore.getInstance("jks");
        try (InputStream in = new FileInputStream(DIFFERENT_CLIENT_TRUSTSTORE_PATH)) {
            differentTruststore.load(in, PASSWORD.toCharArray());
        }
    }

    @BeforeEach
    public void startContainer() throws Exception {
        if (!containerController.isStarted(SSL_CONTAINER_QUALIFIER)) {
            containerController.start(SSL_CONTAINER_QUALIFIER);
            secureServer(SERVER_KEYSTORE_PATH, SSL_CONTAINER_PORT_OFFSET);
            deployer.deploy(DEPLOYMENT_NAME);
        }
    }

    /**
     * @tpTestDetails Trusted server
     *                Client has truststore containing self-signed certificate.
     *                Server/endpoint is secured with the same self-signed certificate.
     * @tpSince RESTEasy 3.7.0
     */
    @Test
    @DisplayName("Test Trusted Server")
    public void testTrustedServer() {
        QuarkusRestClientBuilder = (QuarkusRestClientBuilder) ClientBuilder.newBuilder();
        QuarkusRestClientBuilder.setIsTrustSelfSignedCertificates(false);
        client = QuarkusRestClientBuilder.trustStore(correctTruststore).build();
        Response response = client.target(URL).request().get();
        Assertions.assertEquals(response.readEntity(String.class), "Hello World!");
        Assertions.assertEquals(200, response.getStatus());
    }

    /**
     * @tpTestDetails Untrusted server
     *                Client has truststore containing self-signed certificate.
     *                Server/endpoint is secured with different self-signed certificate so exception should be thrown.
     * @tpSince RESTEasy 3.7.0
     */
    @Test
    @DisplayName("Test Untrusted Server")
    public void testUntrustedServer() {
        assertThrows(ProcessingException.class, () -> {
            QuarkusRestClientBuilder = (QuarkusRestClientBuilder) ClientBuilder.newBuilder();
            QuarkusRestClientBuilder.setIsTrustSelfSignedCertificates(false);
            client = QuarkusRestClientBuilder.trustStore(differentTruststore).build();
            client.target(URL).request().get();
        });
    }

    /**
     * @tpTestDetails Client with no truststore
     *                Server/endpoint is secured with self-signed certificate.
     *                Client has no truststore so it does not trust the server.
     * @tpSince RESTEasy 3.7.0
     */
    @Test
    @DisplayName("Test Client Without Truststore")
    public void testClientWithoutTruststore() {
        assertThrows(ProcessingException.class, () -> {
            QuarkusRestClientBuilder = (QuarkusRestClientBuilder) ClientBuilder.newBuilder();
            QuarkusRestClientBuilder.setIsTrustSelfSignedCertificates(false);
            client = QuarkusRestClientBuilder.build();
            client.target(URL).request().get();
        });
    }

    /**
     * @tpTestDetails Custom SSLContext
     *                Client has truststore containing self-signed certificate.
     *                Server/endpoint is secured with the same self-signed certificate.
     * @tpSince RESTEasy 3.7.0
     */
    @Test
    @DisplayName("Test Custom SSL Context")
    public void testCustomSSLContext() throws Exception {
        QuarkusRestClientBuilder = (QuarkusRestClientBuilder) ClientBuilder.newBuilder();
        QuarkusRestClientBuilder.setIsTrustSelfSignedCertificates(false);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[] { new CustomTrustManager(correctTruststore) }, null);
        client = QuarkusRestClientBuilder.sslContext(sslContext).build();
        Response response = client.target(URL).request().get();
        Assertions.assertEquals(response.readEntity(String.class), "Hello World!");
        Assertions.assertEquals(200, response.getStatus());
    }

    /**
     * @tpTestDetails HostnameVerificationPolicy.STRICT test
     *                Client has truststore containing self-signed certificate.
     *                Server/endpoint is secured with the same self-signed certificate and server hostname (localhost) is
     *                included among 'subject alternative names' in the certificate.
     *                HostnameVerificationPolicy is set to STRICT.
     * @tpSince RESTEasy 3.7.0
     */
    @Test
    @DisplayName("Test Hostname Verification Policy Strict")
    public void testHostnameVerificationPolicyStrict() {
        QuarkusRestClientBuilder = (QuarkusRestClientBuilder) ClientBuilder.newBuilder();
        QuarkusRestClientBuilder.setIsTrustSelfSignedCertificates(false);
        QuarkusRestClientBuilder.hostnameVerification(QuarkusRestClientBuilder.HostnameVerificationPolicy.STRICT);
        client = QuarkusRestClientBuilder.trustStore(correctTruststore).build();
        Response response = client.target(URL).request().get();
        Assertions.assertEquals(response.readEntity(String.class), "Hello World!");
        Assertions.assertEquals(200, response.getStatus());
    }

    /**
     * @tpTestDetails different cert + HostnameVerificationPolicy.ANY test
     *                Client has truststore containing self-signed certificate.
     *                Server/endpoint is secured with different self-signed certificate so exception should be thrown.
     *                HostnameVerificationPolicy is set to ANY but it doesn't matter when certificates doesn't match.
     * @tpSince RESTEasy 3.7.0
     */
    @Test
    @DisplayName("Test Hostname Verification Policy Any")
    public void testHostnameVerificationPolicyAny() {
        assertThrows(ProcessingException.class, () -> {
            QuarkusRestClientBuilder = (QuarkusRestClientBuilder) ClientBuilder.newBuilder();
            QuarkusRestClientBuilder.setIsTrustSelfSignedCertificates(false);
            QuarkusRestClientBuilder.hostnameVerification(QuarkusRestClientBuilder.HostnameVerificationPolicy.ANY);
            client = QuarkusRestClientBuilder.trustStore(differentTruststore).build();
            client.target(URL).request().get();
        });
    }

    /**
     * @tpTestDetails disableTrustManager() test
     *                Client has truststore containing self-signed certificate.
     *                Server/endpoint is secured with different self-signed certificate.
     *                However, disableTrustManager is used so client should trust this certificate (and all others).
     * @tpSince RESTEasy 3.7.0
     */
    @Test
    @DisplayName("Test Disable Trust Manager")
    public void testDisableTrustManager() {
        QuarkusRestClientBuilder = (QuarkusRestClientBuilder) ClientBuilder.newBuilder();
        QuarkusRestClientBuilder.setIsTrustSelfSignedCertificates(false);
        QuarkusRestClientBuilder = QuarkusRestClientBuilder.disableTrustManager();
        client = QuarkusRestClientBuilder.trustStore(differentTruststore).build();
        Response response = client.target(URL).request().get();
        Assertions.assertEquals(response.readEntity(String.class), "Hello World!");
        Assertions.assertEquals(200, response.getStatus());
    }

    /**
     * @tpTestDetails Test for https://issues.jboss.org/browse/RESTEASY-2065
     *                Client has truststore containing self-signed certificate.
     *                Server/endpoint is secured with different self-signed certificate, but by default, all self-signed
     *                certificates should be trusted.
     * @tpSince RESTEasy 3.7.0
     */
    @Test
    @DisplayName("Test Is Trust Self Signed Certificates Default")
    public void testIsTrustSelfSignedCertificatesDefault() {
        QuarkusRestClientBuilder = (QuarkusRestClientBuilder) ClientBuilder.newBuilder();
        client = QuarkusRestClientBuilder.trustStore(differentTruststore).build();
        Response response = client.target(URL).request().get();
        Assertions.assertEquals(response.readEntity(String.class), "Hello World!");
        Assertions.assertEquals(200, response.getStatus());
    }

    /**
     * @tpTestDetails Test for https://issues.jboss.org/browse/RESTEASY-2065
     *                Client has truststore containing self-signed certificate.
     *                Server/endpoint is secured with different self-signed certificate, but after
     *                setIsTrustSelfSignedCertificates(true), all self-signed certificates should be trusted.
     * @tpSince RESTEasy 3.7.0
     */
    @Test
    @DisplayName("Test Is Trust Self Signed Certificates True")
    public void testIsTrustSelfSignedCertificatesTrue() {
        QuarkusRestClientBuilder = (QuarkusRestClientBuilder) ClientBuilder.newBuilder();
        QuarkusRestClientBuilder.setIsTrustSelfSignedCertificates(true);
        client = QuarkusRestClientBuilder.trustStore(differentTruststore).build();
        Response response = client.target(URL).request().get();
        Assertions.assertEquals(response.readEntity(String.class), "Hello World!");
        Assertions.assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("Test Trusted Server With Client Config Provider")
    public void testTrustedServerWithClientConfigProvider() throws IOException, InterruptedException {
        String jarPath = ClientConfigProviderTestJarHelper.createClientConfigProviderTestJarWithSSL();
        File clientTruststore = new File(CLIENT_TRUSTSTORE_PATH);
        Process process = ClientConfigProviderTestJarHelper.runClientConfigProviderTestJar(
                ClientConfigProviderTestJarHelper.TestType.TEST_SSLCONTEXT_USED, jarPath,
                new String[] { URL, clientTruststore.getAbsolutePath() });
        String line = ClientConfigProviderTestJarHelper.getResultOfProcess(process);
        // first request will succeed because SSLContext from ClientConfigProvider will be used. Second request will fail because user will set sslContext on RestEasyBuilder to SSLContext.getDefault()
        Assertions.assertEquals(line, "200");
        process.destroy();
        process = ClientConfigProviderTestJarHelper.runClientConfigProviderTestJar(
                ClientConfigProviderTestJarHelper.TestType.TEST_CLIENTCONFIG_SSLCONTEXT_IGNORED_WHEN_DIFFERENT_SET, jarPath,
                new String[] { URL, clientTruststore.getAbsolutePath() });
        line = ClientConfigProviderTestJarHelper.getResultOfProcess(process);
        Assertions.assertEquals(line, "SSLHandshakeException");
        process.destroy();
        Assertions.assertTrue(new File(jarPath).delete());
    }

    @AfterEach
    public void after() {
        if (client != null) {
            client.close();
        }
    }
}
