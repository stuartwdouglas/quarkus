package io.quarkus.rest.test.cdi.basic;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.cdi.basic.resource.EJBBook;
import io.quarkus.rest.test.cdi.basic.resource.EJBEventsObserver;
import io.quarkus.rest.test.cdi.basic.resource.EJBEventsObserverImpl;
import io.quarkus.rest.test.cdi.basic.resource.EJBEventsSource;
import io.quarkus.rest.test.cdi.basic.resource.EJBEventsSourceImpl;
import io.quarkus.rest.test.cdi.basic.resource.EJBEventsProcessRead;
import io.quarkus.rest.test.cdi.basic.resource.EJBEventsProcessReadWrite;
import io.quarkus.rest.test.cdi.util.UtilityProducer;
import org.jboss.resteasy.utils.PermissionUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import io.quarkus.rest.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.rest.test.simple.TestUtil;

import javax.inject.Inject;
import java.lang.reflect.ReflectPermission;
import java.util.PropertyPermission;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails EJB, Events and RESTEasy integration test.
 * @tpSince RESTEasy 3.0.16
 */
@RunWith(Arquillian.class)
public class EJBEventsTest {
   @Inject
   private Logger log;

   @Inject
   EJBEventsSource eventSource;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      // Arquillian in the deployment

      return war;
   }});

   /**
    * @tpTestDetails Invokes additional methods of JAX-RS resource as local EJB.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testAsLocalEJB() throws Exception {
      log.info("entering testAsLocalEJB()");

      // Create book.
      EJBBook book1 = new EJBBook("RESTEasy: the Sequel");
      int id1 = eventSource.createBook(book1);
      log.info("id1: " + id1);
      assertEquals("Wrong ID of created book", 0, id1);

      // Create another book.
      EJBBook book2 = new EJBBook("RESTEasy: It's Alive");
      int id2 = eventSource.createBook(book2);
      log.info("id2: " + id2);
      assertEquals("Wrong ID of created book", 1, id2);

      // Retrieve first book.
      EJBBook bookResponse1 = eventSource.lookupBookById(id1);
      log.info("book1 response: " + bookResponse1);
      assertEquals("Wrong received book", book1, bookResponse1);

      // Retrieve second book.
      EJBBook bookResponse2 = eventSource.lookupBookById(id2);
      log.info("book2 response: " + bookResponse2);
      assertEquals("Wrong received book", book2, bookResponse2);
   }
}
