package io.quarkus.rest.test.security;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.function.Supplier;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
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
 * @tpTestCaseDetails Tests for SSL - server without certificate
 * @tpSince RESTEasy 3.7.0
 */
@DisplayName("Ssl Server Without Certificate Test")
public class SslServerWithoutCertificateTest extends SslTestBase {

    private static final Logger LOG = Logger.getLogger(SslServerWithoutCertificateTest.class.getName());

    private static KeyStore truststore;

    private static final String CLIENT_TRUSTSTORE_PATH = RESOURCES + "/client.truststore";

    private static final String URL = generateHttpsURL(0, false);

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

    /**
     * @tpTestDetails
     *                Client has truststore containing self-signed certificate.
     *                Server/endpoint is not secured at all. Client should not trust the unsecured server.
     * @tpSince RESTEasy 3.7.0
     */
    @Test
    @DisplayName("Test Server Without Certificate")
    public void testServerWithoutCertificate() {
        assertThrows(ProcessingException.class, () -> {
            QuarkusRestClientBuilder = (QuarkusRestClientBuilder) ClientBuilder.newBuilder();
            QuarkusRestClientBuilder.setIsTrustSelfSignedCertificates(false);
            client = QuarkusRestClientBuilder.trustStore(truststore).build();
            client.target(URL).request().get();
        });
    }

    /**
     * @tpTestDetails
     *                Client has truststore containing self-signed certificate.
     *                Server/endpoint is not secured at all. However, disableTrustManager is used so client should trust this
     *                server.
     * @tpSince RESTEasy 3.7.0
     */
    @Test
    @DisplayName("Test Server Without Certificate Disabled Trust Manager")
    public void testServerWithoutCertificateDisabledTrustManager() {
        QuarkusRestClientBuilder = (QuarkusRestClientBuilder) ClientBuilder.newBuilder();
        QuarkusRestClientBuilder.setIsTrustSelfSignedCertificates(false);
        QuarkusRestClientBuilder = QuarkusRestClientBuilder.disableTrustManager();
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
