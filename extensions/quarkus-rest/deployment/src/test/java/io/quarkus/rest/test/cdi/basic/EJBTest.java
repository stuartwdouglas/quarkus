package io.quarkus.rest.test.cdi.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Hashtable;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.cdi.basic.resource.EJBApplication;
import io.quarkus.rest.test.cdi.basic.resource.EJBBook;
import io.quarkus.rest.test.cdi.basic.resource.EJBBookReader;
import io.quarkus.rest.test.cdi.basic.resource.EJBBookReaderImpl;
import io.quarkus.rest.test.cdi.basic.resource.EJBBookResource;
import io.quarkus.rest.test.cdi.basic.resource.EJBBookWriterImpl;
import io.quarkus.rest.test.cdi.basic.resource.EJBLocalResource;
import io.quarkus.rest.test.cdi.basic.resource.EJBRemoteResource;
import io.quarkus.rest.test.cdi.basic.resource.EJBResourceParent;
import io.quarkus.rest.test.cdi.util.Constants;
import io.quarkus.rest.test.cdi.util.Counter;
import io.quarkus.rest.test.cdi.util.Utilities;
import io.quarkus.rest.test.cdi.util.UtilityProducer;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails EJB and RESTEasy integration test.
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Ejb Test")
public class EJBTest {

    private static Logger log = Logger.getLogger(EJBTest.class);

    @Inject
    EJBLocalResource localResource;

    /**
     * value of DEPLOYMENT_NAME is also used in ejbtest_web.xml file
     */
    public static final String DEPLOYMENT_NAME = "resteasy-ejb-test";

    private Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            // test needs to use special annotations in Application class, TestApplication class could not be used
            war.addClass(EJBApplication.class);
            war.addClass(PortProviderUtil.class);
            war.addClasses(EJBBook.class, Constants.class, Counter.class, UtilityProducer.class, Utilities.class)
                    .addClasses(EJBBookReader.class, EJBBookReaderImpl.class).addClasses(EJBBookWriterImpl.class)
                    .addClasses(EJBResourceParent.class, EJBLocalResource.class, EJBRemoteResource.class, EJBBookResource.class)
                    .setWebXML(EJBTest.class.getPackage(), "ejbtest_web.xml");
            // Arquillian in the deployment
            return war;
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, DEPLOYMENT_NAME);
    }

    /**
     * client needs to be non-static. BeforeClass and AfterClass methods are not executed on server ( annotation is
     * not used).
     */
    @BeforeEach
    public void init() {
        client = ClientBuilder.newClient();
    }

    @AfterEach
    public void close() {
        client.close();
    }

    /**
     * @tpTestDetails Verify that EJBBookReaderImpl, EJBBookWriterImpl, and EJBBookResource
     *                are placed in the correct scope.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Verify Scopes Jax Rs")
    public void testVerifyScopesJaxRs() throws Exception {
        log.info("starting testVerifyScopesJaxRs()");
        WebTarget base = client.target(generateURL("/verifyScopes/"));
        Response response = base.request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus(), "Wrong response status");
        assertEquals(Status.OK.getStatusCode(), response.readEntity(Integer.class).intValue(), "Wrong response content");
    }

    /**
     * @tpTestDetails Verify that EJBBookReaderImpl, EJBBookWriterImpl, and EJBBookResource
     *                are placed in the correct scope on local.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Verify Scopes Local EJB")
    public void testVerifyScopesLocalEJB() throws Exception {
        log.info("starting testVerifyScopesLocalEJB()");
        int result = localResource.verifyScopes();
        assertEquals(Status.OK.getStatusCode(), result);
    }

    /**
     * @tpTestDetails Verify that EJBBookReaderImpl, EJBBookWriterImpl, and EJBBookResource
     *                are placed in the correct scope on remote.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Verify Scopes Remote EJB")
    public void testVerifyScopesRemoteEJB() throws Exception {
        log.info("starting testVerifyScopesRemoteEJB()");
        // Get proxy to JAX-RS resource as EJB.
        EJBRemoteResource remoteResource = getRemoteResource();
        log.info("remote: " + remoteResource);
        int result = remoteResource.verifyScopes();
        log.info("result: " + result);
        assertEquals(Status.OK.getStatusCode(), result);
    }

    /**
     * @tpTestDetails Verify that EJBBookReader and EJBBookWriterImpl are correctly injected
     *                into EJBBookResource.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Verify Injection Jax Rs")
    public void testVerifyInjectionJaxRs() throws Exception {
        log.info("starting testVerifyInjectionJaxRs()");
        WebTarget base = client.target(generateURL("/verifyInjection/"));
        Response response = base.request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus(), "Wrong response status");
        assertEquals(Status.OK.getStatusCode(), response.readEntity(Integer.class).intValue(), "Wrong response content");
    }

    /**
     * @tpTestDetails Verify that EJBBookReader and EJBBookWriterImpl are correctly injected
     *                into EJBBookResource.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Verify Injection Local EJB")
    public void testVerifyInjectionLocalEJB() throws Exception {
        log.info("starting testVerifyInjectionLocalEJB()");
        int result = localResource.verifyInjection();
        log.info("testVerifyInjectionLocalEJB result: " + result);
        assertEquals(Status.OK.getStatusCode(), result);
    }

    /**
     * @tpTestDetails Verify that EJBBookReader and EJBBookWriterImpl are correctly injected
     *                into EJBBookResource.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Verify Injection Remote EJB")
    public void testVerifyInjectionRemoteEJB() throws Exception {
        log.info("starting testVerifyInjectionRemoteEJB()");
        // Get proxy to JAX-RS resource as EJB.
        EJBRemoteResource remoteResource = getRemoteResource();
        log.info("remote: " + remoteResource);
        int result = remoteResource.verifyInjection();
        log.info("result: " + result);
        assertEquals(Status.OK.getStatusCode(), result);
    }

    /**
     * @tpTestDetails Further addresses the use of EJBs as JAX-RS components.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test As Jax RS Resource")
    public void testAsJaxRSResource() throws Exception {
        log.info("entering testAsJaxRSResource()");
        // Create book.
        WebTarget base = client.target(generateURL("/create/"));
        EJBBook book1 = new EJBBook("RESTEasy: the Sequel");
        Response response = base.request().post(Entity.entity(book1, Constants.MEDIA_TYPE_TEST_XML));
        log.info("Status: " + response.getStatus());
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        int id1 = response.readEntity(int.class);
        log.info("id: " + id1);
        assertEquals(Counter.INITIAL_VALUE, id1, "Wrong id of Book1 id");
        // Create another book.
        EJBBook book2 = new EJBBook("RESTEasy: It's Alive");
        response = base.request().post(Entity.entity(book2, Constants.MEDIA_TYPE_TEST_XML));
        log.info("Status: " + response.getStatus());
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        int id2 = response.readEntity(int.class);
        log.info("id: " + id2);
        assertEquals(Counter.INITIAL_VALUE + 1, id2, "Wrong id of Book2 id");
        // Retrieve first book.
        base = client.target(generateURL("/book/" + id1));
        response = base.request().accept(Constants.MEDIA_TYPE_TEST_XML).get();
        log.info("Status: " + response.getStatus());
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        EJBBook result = response.readEntity(EJBBook.class);
        log.info("book: " + book1);
        assertEquals(book1, result, "Wrong book1 received from server");
        // Retrieve second book.
        base = client.target(generateURL("/book/" + id2));
        response = base.request().accept(Constants.MEDIA_TYPE_TEST_XML).get();
        log.info("Status: " + response.getStatus());
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        result = response.readEntity(EJBBook.class);
        log.info("book: " + book2);
        assertEquals(book2, result, "Wrong book2 received from server");
        // Verify that EJBBookReader and EJBBookWriter have been used, twice on each side.
        base = client.target(generateURL("/uses/4"));
        response = base.request().get();
        log.info("Status: " + response.getStatus());
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
        // Reset counter.
        base = client.target(generateURL("/reset"));
        response = base.request().get();
        log.info("Status: " + response.getStatus());
        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Invokes additional methods of JAX-RS resource as local EJB.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test As Local EJB")
    public void testAsLocalEJB() throws Exception {
        log.info("entering testAsLocalEJB()");
        // Create book.
        EJBBook book1 = new EJBBook("RESTEasy: the Sequel");
        int id1 = localResource.createBook(book1);
        log.info("id1: " + id1);
        assertEquals(Counter.INITIAL_VALUE, id1, "Wrong id of Book1 id");
        // Create another book.
        EJBBook book2 = new EJBBook("RESTEasy: It's Alive");
        int id2 = localResource.createBook(book2);
        log.info("id2: " + id2);
        assertEquals(Counter.INITIAL_VALUE + 1, id2, "Wrong id of Book2 id");
        // Retrieve first book.
        EJBBook bookResponse1 = localResource.lookupBookById(id1);
        log.info("book1 response: " + bookResponse1);
        assertEquals(book1, bookResponse1, "Wrong book1 received from server");
        // Retrieve second book.
        EJBBook bookResponse2 = localResource.lookupBookById(id2);
        log.info("book2 response: " + bookResponse2);
        assertEquals(book2, bookResponse2, "Wrong book2 received from server");
        // Verify that EJBBookReader and EJBBookWriter haven't been used.
        localResource.testUse(0);
        // Reset counter.
        localResource.reset();
    }

    /**
     * @tpTestDetails Invokes additional methods of JAX-RS resource as remote EJB.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test As Remote EJB")
    public void testAsRemoteEJB() throws Exception {
        log.info("entering testAsRemoteEJB()");
        // Get proxy to JAX-RS resource as EJB.
        EJBRemoteResource remoteResource = getRemoteResource();
        log.info("remote: " + remoteResource);
        // Create book.
        EJBBook book1 = new EJBBook("RESTEasy: the Sequel");
        int id1 = remoteResource.createBook(book1);
        log.info("id1: " + id1);
        assertEquals(Counter.INITIAL_VALUE, id1, "Wrong id of Book1 id");
        // Create another book.
        EJBBook book2 = new EJBBook("RESTEasy: It's Alive");
        int id2 = remoteResource.createBook(book2);
        log.info("id2: " + id2);
        assertEquals(Counter.INITIAL_VALUE + 1, id2, "Wrong id of Book2 id");
        // Retrieve first book.
        EJBBook bookResponse1 = remoteResource.lookupBookById(id1);
        log.info("book1 response: " + bookResponse1);
        assertEquals(book1, bookResponse1, "Wrong book1 received from server");
        // Retrieve second book.
        EJBBook bookResponse2 = remoteResource.lookupBookById(id2);
        log.info("book2 response: " + bookResponse2);
        assertEquals(book2, bookResponse2, "Wrong book2 received from server");
        // Verify that EJBBookReader and EJBBookWriter haven't been used.
        remoteResource.testUse(0);
        // Reset counter.
        remoteResource.reset();
    }

    private static EJBRemoteResource getRemoteResource() throws Exception {
        final Hashtable<String, String> jndiProperties = new Hashtable<>();
        jndiProperties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        final Context context = new InitialContext(jndiProperties);
        String name = "ejb:/" + DEPLOYMENT_NAME + "/EJBBookResource!" + EJBRemoteResource.class.getName();
        return EJBRemoteResource.class.cast(context.lookup(name));
    }
}
