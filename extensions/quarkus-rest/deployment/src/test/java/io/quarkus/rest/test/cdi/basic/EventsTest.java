package io.quarkus.rest.test.cdi.basic;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import io.quarkus.rest.test.cdi.basic.resource.EventsBookReaderInterceptor;
import io.quarkus.rest.test.cdi.basic.resource.EventsBookWriterInterceptor;
import io.quarkus.rest.test.cdi.basic.resource.EJBBook;
import io.quarkus.rest.test.cdi.basic.resource.EventResource;
import io.quarkus.rest.test.cdi.basic.resource.EventsBookReader;
import io.quarkus.rest.test.cdi.basic.resource.EventsBookWriter;
import io.quarkus.rest.test.cdi.basic.resource.EventsRead;
import io.quarkus.rest.test.cdi.basic.resource.EventsUnused;
import io.quarkus.rest.test.cdi.basic.resource.EventsWrite;
import io.quarkus.rest.test.cdi.basic.resource.EventsReadIntercept;
import io.quarkus.rest.test.cdi.basic.resource.EventsWriteIntercept;
import io.quarkus.rest.test.cdi.util.Constants;
import io.quarkus.rest.test.cdi.util.UtilityProducer;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import io.quarkus.rest.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.rest.test.simple.TestUtil;

import javax.swing.text.Utilities;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;


/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test integration of Events and RESTEasy.
 * @tpSince RESTEasy 3.0.16
 */
public class EventsTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return war;
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, EventsTest.class.getSimpleName());
   }

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

   /**
    * @tpTestDetails Test creating object on resource and retrieving this object again.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testEvents() throws Exception {
      Client client = ClientBuilder.newClient();

      // Create book.
      WebTarget base = client.target(generateURL("/create/"));
      EJBBook book = new EJBBook("RESTEasy: the Sequel");
      Response response = base.request().post(Entity.entity(book, Constants.MEDIA_TYPE_TEST_XML_TYPE));
      assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      int id = response.readEntity(int.class);
      assertEquals("Received wrong id of stored book", 0, id);
      response.close();

      // Retrieve book.
      WebTarget base2 = client.target(generateURL("/book/" + id));
      Response response2 = base2.request().accept(Constants.MEDIA_TYPE_TEST_XML).get();
      assertEquals(HttpResponseCodes.SC_OK, response2.getStatus());
      EJBBook result = response2.readEntity(EJBBook.class);
      assertEquals("Received wrong Book", book, result);
      response2.close();

      // test events
      WebTarget base3 = client.target(generateURL("/test/"));
      Response response3 = base3.request().post(Entity.text(new String()));
      assertEquals(HttpResponseCodes.SC_OK, response3.getStatus());
      response3.close();

      client.close();
   }
}
