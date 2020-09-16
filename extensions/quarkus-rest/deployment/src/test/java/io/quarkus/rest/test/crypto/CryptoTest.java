package io.quarkus.rest.test.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Base64;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.security.KeyTools;
import org.jboss.resteasy.security.smime.EnvelopedInput;
import org.jboss.resteasy.security.smime.EnvelopedOutput;
import org.jboss.resteasy.security.smime.PKCS7SignatureInput;
import org.jboss.resteasy.security.smime.SignedInput;
import org.jboss.resteasy.security.smime.SignedOutput;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.rest.test.crypto.resource.CryptoCertResource;
import io.quarkus.rest.test.crypto.resource.CryptoEncryptedResource;
import io.quarkus.rest.test.crypto.resource.CryptoEncryptedSignedResource;
import io.quarkus.rest.test.crypto.resource.CryptoPkcs7SignedResource;
import io.quarkus.rest.test.crypto.resource.CryptoSignedResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;

/**
 * @tpSubChapter Crypto
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for response secured by BouncyCastleProvider
 * @tpSince RESTEasy 3.0.16
 */
@SuppressWarnings(value = "unchecked")
@DisplayName("Crypto Test")
public class CryptoTest {

    private static final String ERROR_CONTENT_MSG = "Wrong content of response";

    private static final String ERROR_CORE_MSG = "Wrong BouncyCastleProvider and RESTEasy integration";

    private static Logger logger = Logger.getLogger(CryptoTest.class);

    Client client;

    public static X509Certificate cert;

    public static PrivateKey privateKey;

    @BeforeEach
    public void before() throws Exception {
        client = ClientBuilder.newClient();
    }

    @AfterEach
    public void close() {
        client.close();
    }

    @Deployment
    public static Archive<?> deploy() throws IOException {
        WebArchive war = TestUtil.prepareArchive(CryptoTest.class.getSimpleName());
        try {
            BouncyCastleProvider bouncyCastleProvider = new BouncyCastleProvider();
            Security.addProvider(bouncyCastleProvider);
            KeyPair keyPair = KeyPairGenerator.getInstance("RSA", "BC").generateKeyPair();
            privateKey = keyPair.getPrivate();
            cert = KeyTools.generateTestCertificate(keyPair);
            String privateKeyString = toString(privateKey);
            String certString = toString(cert);
            war.addAsResource(new StringAsset(privateKeyString), "privateKey.txt");
            war.addAsResource(new StringAsset(certString), "cert.txt");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        war.addAsManifestResource("jboss-deployment-structure-bouncycastle.xml", "jboss-deployment-structure.xml");
        return TestUtil.finishContainerPrepare(war, null, CryptoEncryptedResource.class, CryptoSignedResource.class,
                CryptoEncryptedSignedResource.class, CryptoPkcs7SignedResource.class, CryptoCertResource.class);
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, CryptoTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Check signed output
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Signed Output")
    public void testSignedOutput() throws Exception {
        Response res = client.target(generateURL("/smime/signed")).request().get();
        SignedInput signed = res.readEntity(SignedInput.class);
        String output = (String) signed.getEntity(String.class);
        Assertions.assertEquals(Status.OK.getStatusCode(), res.getStatus());
        logger.info(output);
        Assertions.assertEquals(ERROR_CONTENT_MSG, "hello world", output);
        Assertions.assertTrue(ERROR_CORE_MSG, signed.verify(cert));
        MediaType contentType = MediaType.valueOf(res.getHeaderString("Content-Type"));
        logger.info(contentType);
        res.close();
    }

    /**
     * @tpTestDetails Check PKCS7 signed output
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test PKCS 7 Signed Output")
    public void testPKCS7SignedOutput() throws Exception {
        Response res = client.target(generateURL("/smime/pkcs7-signature")).request().get();
        PKCS7SignatureInput signed = res.readEntity(PKCS7SignatureInput.class);
        String output = (String) signed.getEntity(String.class, MediaType.TEXT_PLAIN_TYPE);
        logger.info(output);
        Assertions.assertEquals(ERROR_CONTENT_MSG, "hello world", output);
        res.close();
    }

    /**
     * @tpTestDetails Check PKCS7 signed text output
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test PKCS 7 Signed Text Output")
    public void testPKCS7SignedTextOutput() throws Exception {
        Response res = client.target(generateURL("/smime/pkcs7-signature/text")).request().get();
        String base64 = res.readEntity(String.class);
        logger.info(base64);
        PKCS7SignatureInput signed = new PKCS7SignatureInput(base64);
        ResteasyProviderFactory rpf = ResteasyProviderFactory.newInstance();
        RegisterBuiltin.register(rpf);
        signed.setProviders(rpf);
        String output = (String) signed.getEntity(String.class, MediaType.TEXT_PLAIN_TYPE);
        logger.info(output);
        Assertions.assertEquals(ERROR_CONTENT_MSG, "hello world", output);
        Assertions.assertTrue(ERROR_CORE_MSG, signed.verify(cert));
        res.close();
    }

    /**
     * @tpTestDetails Check encrypted output
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Encrypted Output")
    public void testEncryptedOutput() throws Exception {
        Response res = client.target(generateURL("/smime/encrypted")).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), res.getStatus(), "Unexpected BouncyCastle error");
        MediaType contentType = MediaType.valueOf(res.getHeaderString("Content-Type"));
        logger.info(contentType);
        EnvelopedInput enveloped = res.readEntity(EnvelopedInput.class);
        String output = (String) enveloped.getEntity(String.class, privateKey, cert);
        logger.info(output);
        Assertions.assertEquals(ERROR_CONTENT_MSG, "hello world", output);
        res.close();
    }

    /**
     * @tpTestDetails Check write encrypted signed output to file
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Encrypted Signed Output To File")
    public void testEncryptedSignedOutputToFile() throws Exception {
        Response res = client.target(generateURL("/smime/encrypted/signed")).request().get();
        MediaType contentType = MediaType.valueOf(res.getHeaderString("Content-Type"));
        logger.info(contentType);
        logger.info(res.getEntity());
        FileOutputStream os = new FileOutputStream("target/python_encrypted_signed.txt");
        os.write("Content-Type: ".getBytes());
        os.write(contentType.toString().getBytes());
        os.write("\r\n".getBytes());
        os.write(res.readEntity(String.class).getBytes());
        os.close();
        res.close();
    }

    /**
     * @tpTestDetails Check encrypted signed output
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Encrypted Signed Output")
    public void testEncryptedSignedOutput() throws Exception {
        try {
            Response res = client.target(generateURL("/smime/encrypted/signed")).request().get();
            EnvelopedInput enveloped = res.readEntity(EnvelopedInput.class);
            SignedInput signed = (SignedInput) enveloped.getEntity(SignedInput.class, privateKey, cert);
            String output = (String) signed.getEntity(String.class);
            logger.info(output);
            Assertions.assertEquals(ERROR_CONTENT_MSG, "hello world", output);
            Assertions.assertTrue(ERROR_CORE_MSG, signed.verify(cert));
            Assertions.assertEquals(ERROR_CONTENT_MSG, "hello world", output);
            res.close();
        } catch (Exception e) {
            throw new RuntimeException("Unexpected BouncyCastle error", e);
        }
    }

    /**
     * @tpTestDetails Check encrypted input
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Encrypted Input")
    public void testEncryptedInput() throws Exception {
        EnvelopedOutput output = new EnvelopedOutput("input", "text/plain");
        output.setCertificate(cert);
        Response res = client.target(generateURL("/smime/encrypted")).request().post(Entity.entity(output, "*/*"));
        Assertions.assertEquals(Status.NO_CONTENT.getStatusCode(), res.getStatus());
        res.close();
    }

    /**
     * @tpTestDetails Check encrypted signed input
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Encrypted Signed Input")
    public void testEncryptedSignedInput() throws Exception {
        SignedOutput signed = new SignedOutput("input", "text/plain");
        signed.setPrivateKey(privateKey);
        signed.setCertificate(cert);
        EnvelopedOutput output = new EnvelopedOutput(signed, "multipart/signed");
        output.setCertificate(cert);
        Response res = client.target(generateURL("/smime/encrypted/signed")).request().post(Entity.entity(output, "*/*"));
        Assertions.assertEquals(Status.NO_CONTENT.getStatusCode(), res.getStatus());
        res.close();
    }

    /**
     * @tpTestDetails Check signed input
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Signed Input")
    public void testSignedInput() throws Exception {
        SignedOutput output = new SignedOutput("input", "text/plain");
        output.setCertificate(cert);
        output.setPrivateKey(privateKey);
        Response res = client.target(generateURL("/smime/signed")).request().post(Entity.entity(output, "multipart/signed"));
        Assertions.assertEquals(Status.NO_CONTENT.getStatusCode(), res.getStatus());
        res.close();
    }

    @Test
    @DisplayName("Test PKCS 7 Signed Input")
    public void testPKCS7SignedInput() throws Exception {
        try {
            SignedOutput output = new SignedOutput("input", "text/plain");
            output.setCertificate(cert);
            output.setPrivateKey(privateKey);
            Response res = client.target(generateURL("/smime/pkcs7-signature")).request()
                    .post(Entity.entity(output, "application/pkcs7-signature"));
            Assertions.assertEquals(Status.NO_CONTENT.getStatusCode(), res.getStatus());
            res.close();
        } catch (Exception e) {
            throw new RuntimeException("Unexpected BouncyCastle error", e);
        }
    }

    /**
     * Read the object from Base64 string.
     */
    private static Object fromString(String s) throws IOException, ClassNotFoundException {
        byte[] data = Base64.getDecoder().decode(s);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
        Object o = ois.readObject();
        ois.close();
        return o;
    }

    /**
     * Write the object to a Base64 string.
     */
    private static String toString(Serializable o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        oos.close();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}
