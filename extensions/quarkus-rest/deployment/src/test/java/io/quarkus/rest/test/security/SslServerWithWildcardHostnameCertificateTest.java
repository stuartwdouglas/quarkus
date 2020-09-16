package io.quarkus.rest.test.security;

import static io.quarkus.rest.test.ContainerConstants.SSL_CONTAINER_PORT_OFFSET_WILDCARD;
import static io.quarkus.rest.test.ContainerConstants.SSL_CONTAINER_QUALIFIER_WILDCARD;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.function.Supplier;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.logging.Logger;
import org.jboss.resteasy.utils.TestUtil;
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
 * @tpTestCaseDetails Tests for SSL - server secured with certificate with wildcard hostname "*host"
 * @tpSince RESTEasy 3.7.0
 */
@DisplayName("Ssl Server With Wildcard Hostname Certificate Test")
public class SslServerWithWildcardHostnameCertificateTest extends SslTestBase {

    private static final Logger LOG = Logger.getLogger(SslServerWithWildcardHostnameCertificateTest.class.getName());

    private static KeyStore truststore;

    private static final String SERVER_KEYSTORE_PATH = RESOURCES + "/server-wildcard-hostname.keystore";

    private static final String CLIENT_TRUSTSTORE_PATH = RESOURCES + "/client-wildcard-hostname.truststore";

    private static final String URL = generateHttpsURL(SSL_CONTAINER_PORT_OFFSET_WILDCARD);

    @TargetsContainer(SSL_CONTAINER_QUALIFIER_WILDCARD)
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
        if (!containerController.isStarted(SSL_CONTAINER_QUALIFIER_WILDCARD)) {
            containerController.start(SSL_CONTAINER_QUALIFIER_WILDCARD);
            secureServer(SERVER_KEYSTORE_PATH, SSL_CONTAINER_PORT_OFFSET_WILDCARD);
            deployer.deploy(DEPLOYMENT_NAME);
        }
    }

    /**
     * @tpTestDetails HostnameVerificationPolicy.WILDCARD test
     *                Client has truststore containing self-signed certificate.
     *                Server/endpoint is secured with the same self-signed certificate, but only wildcard of server hostname
     *                (*host) is included among 'subject alternative names' in the certificate.
     *                Client should trust the server because HostnameVerificationPolicy is set to WILDCARD.
     * @tpSince RESTEasy 3.7.0
     */
    @Test
    @DisplayName("Test Hostname Verification Policy Wildcard")
    public void testHostnameVerificationPolicyWildcard() {
        QuarkusRestClientBuilder = (QuarkusRestClientBuilder) ClientBuilder.newBuilder();
        QuarkusRestClientBuilder.setIsTrustSelfSignedCertificates(false);
        QuarkusRestClientBuilder.hostnameVerification(QuarkusRestClientBuilder.HostnameVerificationPolicy.WILDCARD);
        client = QuarkusRestClientBuilder.trustStore(truststore).build();
        Response response = client.target(URL).request().get();
        Assertions.assertEquals(response.readEntity(String.class), "Hello World!");
        Assertions.assertEquals(200, response.getStatus());
    }

    /**
     * @tpTestDetails HostnameVerificationPolicy.STRICT test
     *                Client has truststore containing self-signed certificate.
     *                Server/endpoint is secured with the same self-signed certificate, but only wildcard of server hostname
     *                (*host) is included among 'subject alternative names' in the certificate.
     *                HostnameVerificationPolicy is set to STRICT so exception should be thrown.
     * @tpSince RESTEasy 3.7.0
     */
    @Test
    @DisplayName("Test Hostname Verification Policy Strict")
    public void testHostnameVerificationPolicyStrict() throws Exception {
        QuarkusRestClientBuilder = (QuarkusRestClientBuilder) ClientBuilder.newBuilder();
        QuarkusRestClientBuilder.setIsTrustSelfSignedCertificates(false);
        QuarkusRestClientBuilder.hostnameVerification(QuarkusRestClientBuilder.HostnameVerificationPolicy.STRICT);
        client = QuarkusRestClientBuilder.trustStore(truststore).build();
        try {
            if (InetAddress.getByName("localhost.localdomain") != null) {
                String anotherURL = URL.replace("localhost", "localhost.localdomain");
                try {
                    client.target(anotherURL).request().get();
                    Assert.fail("ProcessingException ie expected");
                } catch (ProcessingException e) {
                    // expected
                }
            }
        } catch (UnknownHostException e) {
            try {
                if (InetAddress.getByName("localhost.localhost") != null) {
                    String anotherURL = URL.replace("localhost", "localhost.localhost");
                    try {
                        client.target(anotherURL).request().get();
                        Assert.fail("ProcessingException ie expected");
                    } catch (ProcessingException e1) {
                        // expected
                    }
                }
            } catch (UnknownHostException e2) {
                LOG.warn("Neither 'localhost.localdomain' nor 'local.localhost'can be resolved, " + "nothing is checked");
            }
        }
    }

    @AfterEach
    public void after() {
        client.close();
    }
}
