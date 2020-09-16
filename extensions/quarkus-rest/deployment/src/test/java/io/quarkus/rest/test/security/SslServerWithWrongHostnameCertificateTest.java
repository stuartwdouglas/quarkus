package io.quarkus.rest.test.security;

import static io.quarkus.rest.test.ContainerConstants.SSL_CONTAINER_PORT_OFFSET_WRONG;
import static io.quarkus.rest.test.ContainerConstants.SSL_CONTAINER_QUALIFIER_WRONG;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.function.Supplier;

import javax.net.ssl.HostnameVerifier;
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
import io.quarkus.rest.test.security.resource.SslResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Security
 * @tpChapter Integration tests
 * @tpTestCaseDetails Tests for SSL - server secured with certificate with wrong hostname "abc"
 * @tpSince RESTEasy 3.7.0
 */
@DisplayName("Ssl Server With Wrong Hostname Certificate Test")
public class SslServerWithWrongHostnameCertificateTest extends SslTestBase {

    private static final Logger LOG = Logger.getLogger(SslServerWithWrongHostnameCertificateTest.class.getName());

    private static KeyStore truststore;

    private static final String SERVER_KEYSTORE_PATH = RESOURCES + "/server-wrong-hostname.keystore";

    private static final String CLIENT_TRUSTSTORE_PATH = RESOURCES + "/client-wrong-hostname.truststore";

    private static final String URL = generateHttpsURL(SSL_CONTAINER_PORT_OFFSET_WRONG);

    @TargetsContainer(SSL_CONTAINER_QUALIFIER_WRONG)
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
    public static void prepareTruststore()
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        truststore = KeyStore.getInstance("jks");
        try (InputStream in = new FileInputStream(CLIENT_TRUSTSTORE_PATH)) {
            truststore.load(in, PASSWORD.toCharArray());
        }
    }

    @BeforeEach
    public void startContainer() throws Exception {
        if (!containerController.isStarted(SSL_CONTAINER_QUALIFIER_WRONG)) {
            containerController.start(SSL_CONTAINER_QUALIFIER_WRONG);
            secureServer(SERVER_KEYSTORE_PATH, SSL_CONTAINER_PORT_OFFSET_WRONG);
            deployer.deploy(DEPLOYMENT_NAME);
        }
    }

    /**
     * @tpTestDetails HostnameVerificationPolicy.STRICT test
     *                Client has truststore containing self-signed certificate.
     *                Server/endpoint is secured with the same self-signed certificate but server hostname(localhost) is not
     *                included among 'subject alternative names' in the certificate.
     *                HostnameVerificationPolicy is set to STRICT so exception should be thrown.
     * @tpSince RESTEasy 3.7.0
     */
    @Test
    @DisplayName("Test Hostname Verification Policy Strict")
    public void testHostnameVerificationPolicyStrict() {
        assertThrows(ProcessingException.class, () -> {
            QuarkusRestClientBuilder = (QuarkusRestClientBuilder) ClientBuilder.newBuilder();
            QuarkusRestClientBuilder.setIsTrustSelfSignedCertificates(false);
            QuarkusRestClientBuilder.hostnameVerification(QuarkusRestClientBuilder.HostnameVerificationPolicy.STRICT);
            client = QuarkusRestClientBuilder.trustStore(truststore).build();
            client.target(URL).request().get();
        });
    }

    /**
     * @tpTestDetails HostnameVerificationPolicy.WILDCARD test
     *                Client has truststore containing self-signed certificate.
     *                Server/endpoint is secured with the same self-signed certificate but server hostname(localhost) is not
     *                included among 'subject alternative names' in the certificate.
     *                HostnameVerificationPolicy is set to WILDCARD so exception should be thrown.
     * @tpSince RESTEasy 3.7.0
     */
    @Test
    @DisplayName("Test Hostname Verification Policy Wildcard")
    public void testHostnameVerificationPolicyWildcard() {
        assertThrows(ProcessingException.class, () -> {
            QuarkusRestClientBuilder = (QuarkusRestClientBuilder) ClientBuilder.newBuilder();
            QuarkusRestClientBuilder.setIsTrustSelfSignedCertificates(false);
            QuarkusRestClientBuilder.hostnameVerification(QuarkusRestClientBuilder.HostnameVerificationPolicy.WILDCARD);
            client = QuarkusRestClientBuilder.trustStore(truststore).build();
            client.target(URL).request().get();
        });
    }

    /**
     * @tpTestDetails HostnameVerificationPolicy.ANY test
     *                Client has truststore containing self-signed certificate.
     *                Server/endpoint is secured with the same self-signed certificate and server hostname(localhost) is not
     *                included among 'subject alternative names' in the certificate.
     *                Client should trust the server because HostnameVerificationPolicy is set to ANY.
     * @tpSince RESTEasy 3.7.0
     */
    @Test
    @DisplayName("Test Hostname Verification Policy Any")
    public void testHostnameVerificationPolicyAny() {
        QuarkusRestClientBuilder = (QuarkusRestClientBuilder) ClientBuilder.newBuilder();
        QuarkusRestClientBuilder.setIsTrustSelfSignedCertificates(false);
        QuarkusRestClientBuilder.hostnameVerification(QuarkusRestClientBuilder.HostnameVerificationPolicy.ANY);
        client = QuarkusRestClientBuilder.trustStore(truststore).build();
        Response response = client.target(URL).request().get();
        Assertions.assertEquals(response.readEntity(String.class), "Hello World!");
        Assertions.assertEquals(200, response.getStatus());
    }

    /**
     * @tpTestDetails custom hostnameVerifier
     *                Client has truststore containing self-signed certificate.
     *                Server/endpoint is secured with the same self-signed certificate and server hostname(localhost) is not
     *                included among 'subject alternative names' in the certificate.
     *                Instead it was generated for hostname "abc".
     *                Client should trust the server because custom HostnameVerifier is configured to return true for localhost.
     * @tpSince RESTEasy 3.7.0
     */
    @Test
    @DisplayName("Test Custom Hostname Verifier")
    public void testCustomHostnameVerifier() {
        QuarkusRestClientBuilder = (QuarkusRestClientBuilder) ClientBuilder.newBuilder();
        QuarkusRestClientBuilder.setIsTrustSelfSignedCertificates(false);
        HostnameVerifier hostnameVerifier = (s, sslSession) -> s.equals(HOSTNAME);
        QuarkusRestClientBuilder.hostnameVerifier(hostnameVerifier);
        client = QuarkusRestClientBuilder.trustStore(truststore).build();
        Response response = client.target(URL).request().get();
        Assertions.assertEquals(response.readEntity(String.class), "Hello World!");
        Assertions.assertEquals(200, response.getStatus());
    }

    /**
     * @tpTestDetails custom hostnameVerifier - accept all
     *                Client has truststore containing self-signed certificate.
     *                Server/endpoint is secured with the same self-signed certificate and server actual hostname(localhost) is
     *                not included among 'subject alternative names' in the certificate.
     *                Client should trust the server because HostnameVerifier acceptAll is configured to return true every time.
     * @tpSince RESTEasy 3.7.0
     */
    @Test
    @DisplayName("Test Custom Hostname Verifier Accept All")
    public void testCustomHostnameVerifierAcceptAll() {
        QuarkusRestClientBuilder = (QuarkusRestClientBuilder) ClientBuilder.newBuilder();
        QuarkusRestClientBuilder.setIsTrustSelfSignedCertificates(false);
        HostnameVerifier acceptAll = (hostname, session) -> true;
        QuarkusRestClientBuilder.hostnameVerifier(acceptAll);
        client = QuarkusRestClientBuilder.trustStore(truststore).build();
        Response response = client.target(URL).request().get();
        Assertions.assertEquals(response.readEntity(String.class), "Hello World!");
        Assertions.assertEquals(200, response.getStatus());
    }

    @AfterEach
    public void after() {
        client.close();
    }
}
