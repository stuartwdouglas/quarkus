package io.quarkus.rest.test.cdi.basic;

import static org.junit.Assert.assertEquals;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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
public class DecoratorsTest {

    private static Logger log = Logger.getLogger(DecoratorsTest.class);

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return war;
                }
            });

    private ResteasyProviderFactory factory;

    @Before
    public void setup() {
        // Create an instance and set it as the singleton to use
        factory = ResteasyProviderFactory.newInstance();
        ResteasyProviderFactory.setInstance(factory);
        RegisterBuiltin.register(factory);
    }

    @After
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
    public void testDecorators() throws Exception {
        Client client = ClientBuilder.newClient();

        // Create book.
        WebTarget base = client.target(generateURL("/create/"));
        EJBBook book = new EJBBook("RESTEasy: the Sequel");
        Response response = base.request().post(Entity.entity(book, Constants.MEDIA_TYPE_TEST_XML_TYPE));
        assertEquals(Status.OK, response.getStatus());
        log.info("Status: " + response.getStatus());
        int id = response.readEntity(int.class);
        log.info("id: " + id);
        assertEquals("Wrong id of received book", 0, id);
        response.close();

        // Retrieve book.
        base = client.target(generateURL("/book/" + id));
        response = base.request().accept(Constants.MEDIA_TYPE_TEST_XML).get();
        assertEquals(Status.OK, response.getStatus());
        EJBBook result = response.readEntity(EJBBook.class);
        log.info("book: " + book);
        assertEquals("Wrong received book", book, result);
        response.close();

        // Test order of decorator invocations.
        base = client.target(generateURL("/test/"));
        response = base.request().post(Entity.text(new String()));
        assertEquals("Wrong decorator usage", Status.OK, response.getStatus());
        response.close();

        client.close();
    }
}
