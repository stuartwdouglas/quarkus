package io.quarkus.rest.test.cdi.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.cdi.basic.resource.EJBBook;
import io.quarkus.rest.test.cdi.util.Constants;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for integration of RESTEasy and CDI decorators.
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Decorators Test")
public class DecoratorsTest {

    private static Logger log = Logger.getLogger(DecoratorsTest.class);

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return war;
        }
    });

    private ResteasyProviderFactory factory;

    @BeforeEach
    public void setup() {
        // Create an instance and set it as the singleton to use
        factory = ResteasyProviderFactory.newInstance();
        ResteasyProviderFactory.setInstance(factory);
        RegisterBuiltin.register(factory);
    }

    @AfterEach
    public void cleanup() {
        // Clear the singleton
        ResteasyProviderFactory.clearInstanceIfEqual(factory);
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, DecoratorsTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Store Book to server, received it and check decorator usage.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Decorators")
    public void testDecorators() throws Exception {
        Client client = ClientBuilder.newClient();
        // Create book.
        WebTarget base = client.target(generateURL("/create/"));
        EJBBook book = new EJBBook("RESTEasy: the Sequel");
        Response response = base.request().post(Entity.entity(book, Constants.MEDIA_TYPE_TEST_XML_TYPE));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        log.info("Status: " + response.getStatus());
        int id = response.readEntity(int.class);
        log.info("id: " + id);
        assertEquals(0, id, "Wrong id of received book");
        response.close();
        // Retrieve book.
        base = client.target(generateURL("/book/" + id));
        response = base.request().accept(Constants.MEDIA_TYPE_TEST_XML).get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        EJBBook result = response.readEntity(EJBBook.class);
        log.info("book: " + book);
        assertEquals(book, result, "Wrong received book");
        response.close();
        // Test order of decorator invocations.
        base = client.target(generateURL("/test/"));
        response = base.request().post(Entity.text(new String()));
        assertEquals(Status.OK.getStatusCode(), response.getStatus(), "Wrong decorator usage");
        response.close();
        client.close();
    }
}
