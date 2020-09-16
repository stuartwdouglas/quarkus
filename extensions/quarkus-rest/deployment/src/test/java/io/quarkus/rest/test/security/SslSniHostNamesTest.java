package io.quarkus.rest.test.security;

import static io.quarkus.rest.test.ContainerConstants.SSL_CONTAINER_PORT_OFFSET_SNI;
import static io.quarkus.rest.test.ContainerConstants.SSL_CONTAINER_QUALIFIER_SNI;
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
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import io.quarkus.rest.runtime.client.QuarkusRestClientBuilder;
import io.quarkus.rest.test.security.resource.SslResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Security
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for sniHostNames - method to choose which certificate should be presented by the server
 * @tpSince RESTEasy 3.7.0
 */
@DisplayName("Ssl Sni Host Names Test")
public class SslSniHostNamesTest extends SslTestBase {

    private static final Logger LOG = Logger.getLogger(SslSniHostNamesTest.class.getName());

    private static KeyStore truststore;

    private static String BATCH = RESOURCES + "/ssl-batch-command.txt";

    private static String SERVER_WRONG_KEYSTORE_PATH = RESOURCES + "/server-wrong-hostname.keystore";

    private static String SERVER_TRUSTED_KEYSTORE_PATH = RESOURCES + "/server.keystore";

    private static final String CLIENT_TRUSTSTORE_PATH = RESOURCES + "/client.truststore";

    private static final String URL = generateHttpsURL(SSL_CONTAINER_PORT_OFFSET_SNI);

    @TargetsContainer(SSL_CONTAINER_QUALIFIER_SNI)
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
        if (!containerController.isStarted(SSL_CONTAINER_QUALIFIER_SNI)) {
            containerController.start(SSL_CONTAINER_QUALIFIER_SNI);
            secureServer();
            deployer.deploy(DEPLOYMENT_NAME);
        }
    }

    /**
     * @tpTestDetails Client has truststore containing self-signed certificate.
     *                Server/endpoint has two certificates - managed by two separate SSLContexts. Default SSLContext has wrong
     *                certificate - not trusted by client.
     * @tpSince RESTEasy 3.7.0
     */
    @Test
    @DisplayName("Test Exception")
    public void testException() {
        assertThrows(ProcessingException.class, () -> {
            QuarkusRestClientBuilder = (QuarkusRestClientBuilder) ClientBuilder.newBuilder();
            QuarkusRestClientBuilder.setIsTrustSelfSignedCertificates(false);
            client = QuarkusRestClientBuilder.trustStore(truststore).build();
            client.target(URL).request().get();
        });
    }

    /**
     * @tpTestDetails Client has truststore containing self-signed certificate.
     *                Server/endpoint has two certificates - managed by two separate SSLContexts. Default SSLContext has wrong
     *                certificate - not trusted by client.
     *                However, client requests certificate for localhost using sniHostNames method.
     * @tpSince RESTEasy 3.7.0
     */
    @Test
    @DisplayName("Test")
    public void test() {
        QuarkusRestClientBuilder = (QuarkusRestClientBuilder) ClientBuilder.newBuilder();
        QuarkusRestClientBuilder.setIsTrustSelfSignedCertificates(false);
        QuarkusRestClientBuilder.sniHostNames(HOSTNAME);
        client = QuarkusRestClientBuilder.trustStore(truststore).build();
        Response response = client.target(URL).request().get();
        Assertions.assertEquals(response.readEntity(String.class), "Hello World!");
        Assertions.assertEquals(200, response.getStatus());
    }

    /**
     * Set up ssl in jboss-cli so https endpoint can be accessed only if client trusts certificates in the server keystore.
     *
     * @throws Exception
     */
    private static void secureServer() throws Exception {
        File file = new File(SERVER_WRONG_KEYSTORE_PATH);
        SERVER_WRONG_KEYSTORE_PATH = file.getAbsolutePath();
        file = new File(SERVER_TRUSTED_KEYSTORE_PATH);
        SERVER_TRUSTED_KEYSTORE_PATH = file.getAbsolutePath();
        file = new File(BATCH);
        BATCH = file.getAbsolutePath();
        if (TestUtil.isWindows()) {
            SERVER_WRONG_KEYSTORE_PATH = SERVER_WRONG_KEYSTORE_PATH.replace("\\", "\\\\");
            SERVER_TRUSTED_KEYSTORE_PATH = SERVER_TRUSTED_KEYSTORE_PATH.replace("\\", "\\\\");
            BATCH = BATCH.replace("\\", "\\\\");
        }
        OnlineManagementClient client = TestUtil.clientInit(SSL_CONTAINER_PORT_OFFSET_SNI);
        // create SSLContext with untrusted certificate (hostname is wrong)
        TestUtil.runCmd(client,
                String.format("/subsystem=elytron/key-store=httpsKS:add(path=%s,credential-reference={clear-text=%s},type=JKS)",
                        SERVER_WRONG_KEYSTORE_PATH, PASSWORD));
        TestUtil.runCmd(client,
                String.format(
                        "/subsystem=elytron/key-manager=httpsKM:add(key-store=httpsKS,credential-reference={clear-text=%s})",
                        PASSWORD));
        if (TestUtil.isIbmJdk()) {
            // on ibm java, client doesn't use TLSv1.2
            TestUtil.runCmd(client,
                    "/subsystem=elytron/server-ssl-context=httpsSSC:add(key-manager=httpsKM,protocols=[\"TLSv1\"])");
        } else {
            TestUtil.runCmd(client,
                    "/subsystem=elytron/server-ssl-context=httpsSSC:add(key-manager=httpsKM,protocols=[\"TLSv1.2\"])");
        }
        // create SSLContext with trusted certificate
        TestUtil.runCmd(client,
                String.format(
                        "/subsystem=elytron/key-store=httpsKS1:add(path=%s,credential-reference={clear-text=%s},type=JKS)",
                        SERVER_TRUSTED_KEYSTORE_PATH, PASSWORD));
        TestUtil.runCmd(client,
                String.format(
                        "/subsystem=elytron/key-manager=httpsKM1:add(key-store=httpsKS1,credential-reference={clear-text=%s})",
                        PASSWORD));
        if (TestUtil.isIbmJdk()) {
            TestUtil.runCmd(client,
                    "/subsystem=elytron/server-ssl-context=httpsSSC1:add(key-manager=httpsKM1,protocols=[\"TLSv1\"])");
        } else {
            TestUtil.runCmd(client,
                    "/subsystem=elytron/server-ssl-context=httpsSSC1:add(key-manager=httpsKM1,protocols=[\"TLSv1.2\"])");
        }
        // set untrusted SSLContext as default and trusted SSLContext to be activated with sniHostNames("localhost")
        TestUtil.runCmd(client,
                "/subsystem=elytron/server-ssl-sni-context=test-sni:add(default-ssl-context=httpsSSC,host-context-map={localhost=httpsSSC1})");
        // remove the reference to the legacy security realm and use configuration above instead
        TestUtil.runCmd(client, String.format("run-batch --file=%s", BATCH));
        Administration admin = new Administration(client, 240);
        admin.reload();
        client.close();
    }

    @AfterEach
    public void after() {
        client.close();
    }
}
