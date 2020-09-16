package io.quarkus.rest.test.crypto;

import java.io.FileInputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.resteasy.plugins.providers.multipart.MultipartOutput;
import org.jboss.resteasy.security.PemUtils;
import org.jboss.resteasy.security.smime.EnvelopedOutput;
import org.jboss.resteasy.security.smime.SignedOutput;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.crypto.resource.VerifyDecryptResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;

/**
 * @tpSubChapter Crypto
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-962
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Verify Decrypt Test")
public class VerifyDecryptTest {

    private static final String RESPONSE_ERROR_MSG = "Response contains wrong content";

    protected static final MediaType MULTIPART_MIXED = new MediaType("multipart", "mixed");

    public static X509Certificate cert;

    public static PrivateKey privateKey;

    private static QuarkusRestClient client;

    static final String certPemPath;

    static final String certPrivatePemPath;

    static {
        certPemPath = TestUtil.getResourcePath(VerifyDecryptTest.class, "VerifyDecryptMycert.pem");
        certPrivatePemPath = TestUtil.getResourcePath(VerifyDecryptTest.class, "VerifyDecryptMycertPrivate.pem");
    }

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void close() {
        client.close();
        client = null;
    }

    @Deployment
    public static Archive<?> deploy() throws Exception {
        cert = PemUtils.decodeCertificate(new FileInputStream(certPemPath));
        privateKey = PemUtils.decodePrivateKey(new FileInputStream(certPrivatePemPath));
        WebArchive war = TestUtil.prepareArchive(VerifyDecryptTest.class.getSimpleName());
        war.addAsResource(VerifyDecryptTest.class.getPackage(), "VerifyDecryptMycert.pem", "mycert.pem");
        war.addAsResource(VerifyDecryptTest.class.getPackage(), "VerifyDecryptMycertPrivate.pem", "mycert-private.pem");
        return TestUtil.finishContainerPrepare(war, null, VerifyDecryptResource.class);
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, VerifyDecryptTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Encryption output "application/pkcs7-mime"
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Encrypt")
    public void testEncrypt() throws Exception {
        EnvelopedOutput output = new EnvelopedOutput("xanadu", MediaType.TEXT_PLAIN_TYPE);
        output.setCertificate(cert);
        QuarkusRestWebTarget target = client.target(generateURL("/encrypt"));
        Response res = target.request().post(Entity.entity(output, "application/pkcs7-mime"));
        String result = res.readEntity(String.class);
        Assertions.assertEquals(RESPONSE_ERROR_MSG, "xanadu", result);
    }

    /**
     * @tpTestDetails Signing text/plain output
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Sign")
    public void testSign() throws Exception {
        SignedOutput signed = new SignedOutput("xanadu", MediaType.TEXT_PLAIN_TYPE);
        signed.setPrivateKey(privateKey);
        signed.setCertificate(cert);
        QuarkusRestWebTarget target = client.target(generateURL("/sign"));
        Response res = target.request().post(Entity.entity(signed, "multipart/signed"));
        String result = res.readEntity(String.class);
        Assertions.assertEquals(RESPONSE_ERROR_MSG, "xanadu", result);
    }

    /**
     * @tpTestDetails Encryption and signing test, output type is "application/pkcs7-mime"
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Encrypt Sign")
    public void testEncryptSign() throws Exception {
        EnvelopedOutput output = new EnvelopedOutput("xanadu", MediaType.TEXT_PLAIN_TYPE);
        output.setCertificate(cert);
        SignedOutput signed = new SignedOutput(output, "application/pkcs7-mime");
        signed.setCertificate(cert);
        signed.setPrivateKey(privateKey);
        QuarkusRestWebTarget target = client.target(generateURL("/encryptSign"));
        Response res = target.request().post(Entity.entity(signed, "multipart/signed"));
        String result = res.readEntity(String.class);
        Assertions.assertEquals(RESPONSE_ERROR_MSG, "xanadu", result);
    }

    /**
     * @tpTestDetails Encryption and signing test, output type is "multipart/signed"
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Sign Encrypt")
    public void testSignEncrypt() throws Exception {
        SignedOutput signed = new SignedOutput("xanadu", MediaType.TEXT_PLAIN_TYPE);
        signed.setPrivateKey(privateKey);
        signed.setCertificate(cert);
        EnvelopedOutput output = new EnvelopedOutput(signed, "multipart/signed");
        output.setCertificate(cert);
        QuarkusRestWebTarget target = client.target(generateURL("/signEncrypt"));
        Response res = target.request().post(Entity.entity(output, "application/pkcs7-mime"));
        String result = res.readEntity(String.class);
        Assertions.assertEquals(RESPONSE_ERROR_MSG, "xanadu", result);
    }

    /**
     * @tpTestDetails Encrepted input and output
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Encrypted Encrypted")
    public void testEncryptedEncrypted() {
        MultipartOutput multipart = new MultipartOutput();
        multipart.addPart("xanadu", MediaType.TEXT_PLAIN_TYPE);
        EnvelopedOutput innerPart = new EnvelopedOutput("xanadu", MediaType.TEXT_PLAIN_TYPE);
        innerPart.setCertificate(cert);
        EnvelopedOutput output = new EnvelopedOutput(innerPart, "application/pkcs7-mime");
        output.setCertificate(cert);
        QuarkusRestWebTarget target = client.target(generateURL("/encryptedEncrypted"));
        Response res = target.request().post(Entity.entity(output, "application/pkcs7-mime"));
        String result = res.readEntity(String.class);
        Assertions.assertEquals(RESPONSE_ERROR_MSG, "xanadu", result);
    }

    /**
     * @tpTestDetails Encrepted input and output
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Encrypt Sign Sign")
    public void testEncryptSignSign() throws Exception {
        EnvelopedOutput output = new EnvelopedOutput("xanadu", MediaType.TEXT_PLAIN_TYPE);
        output.setCertificate(cert);
        SignedOutput signed = new SignedOutput(output, "application/pkcs7-mime");
        signed.setCertificate(cert);
        signed.setPrivateKey(privateKey);
        SignedOutput resigned = new SignedOutput(signed, "multipart/signed");
        resigned.setCertificate(cert);
        resigned.setPrivateKey(privateKey);
        QuarkusRestWebTarget target = client.target(generateURL("/encryptSignSign"));
        Response res = target.request().post(Entity.entity(resigned, "multipart/signed"));
        String result = res.readEntity(String.class);
        Assertions.assertEquals(RESPONSE_ERROR_MSG, "xanadu", result);
    }

    /**
     * @tpTestDetails Encrypted multipart output
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Multipart Encrypted")
    public void testMultipartEncrypted() {
        MultipartOutput multipart = new MultipartOutput();
        multipart.addPart("xanadu", MediaType.TEXT_PLAIN_TYPE);
        EnvelopedOutput output = new EnvelopedOutput(multipart, MULTIPART_MIXED);
        output.setCertificate(cert);
        QuarkusRestWebTarget target = client.target(generateURL("/multipartEncrypted"));
        Response res = target.request().post(Entity.entity(output, "application/pkcs7-mime"));
        String result = res.readEntity(String.class);
        Assertions.assertEquals(RESPONSE_ERROR_MSG, "xanadu", result);
    }
}
