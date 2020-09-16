package io.quarkus.rest.test.cdi.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Supplier;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.cdi.basic.resource.EJBBook;
import io.quarkus.rest.test.cdi.basic.resource.EJBEventsSource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails EJB, Events and RESTEasy integration test.
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Ejb Events Test")
public class EJBEventsTest {

    @Inject
    private Logger log;

    @Inject
    EJBEventsSource eventSource;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            // Arquillian in the deployment
            return war;
        }
    });

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
        int id1 = eventSource.createBook(book1);
        log.info("id1: " + id1);
        assertEquals(0, id1, "Wrong ID of created book");
        // Create another book.
        EJBBook book2 = new EJBBook("RESTEasy: It's Alive");
        int id2 = eventSource.createBook(book2);
        log.info("id2: " + id2);
        assertEquals(1, id2, "Wrong ID of created book");
        // Retrieve first book.
        EJBBook bookResponse1 = eventSource.lookupBookById(id1);
        log.info("book1 response: " + bookResponse1);
        assertEquals(book1, bookResponse1, "Wrong received book");
        // Retrieve second book.
        EJBBook bookResponse2 = eventSource.lookupBookById(id2);
        log.info("book2 response: " + bookResponse2);
        assertEquals(book2, bookResponse2, "Wrong received book");
    }
}
