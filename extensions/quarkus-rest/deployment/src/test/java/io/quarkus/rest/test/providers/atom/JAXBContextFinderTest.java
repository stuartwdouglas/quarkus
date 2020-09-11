package io.quarkus.rest.test.providers.atom;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.plugins.providers.atom.Entry;
import org.jboss.resteasy.plugins.providers.atom.Feed;
import io.quarkus.rest.test.providers.atom.resource.JAXBContextFinderAtomServer;
import io.quarkus.rest.test.providers.atom.resource.JAXBContextFinderCustomerAtom;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import io.quarkus.rest.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.rest.test.simple.TestUtil;

import javax.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.util.Iterator;

/**
 * @tpSubChapter Atom provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test integration of atom provider and JAXB Context finder
 * @tpSince RESTEasy 3.0.16
 */
public class JAXBContextFinderTest {

   static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(JAXBContextFinderCustomerAtom.class);
      return TestUtil.finishContainerPrepare(war, null, JAXBContextFinderAtomServer.class);
   }});

   @Before
   public void init() {
      client = (QuarkusRestClient)ClientBuilder.newClient();
   }

   @After
   public void after() throws Exception {
      client.close();
   }

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, JAXBContextFinderTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Test new client
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testAtomFeedNewClient() throws Exception {
      Response response = client.target(generateURL("/atom/feed")).request().get();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      Feed feed = response.readEntity(Feed.class);
      Iterator<Entry> it = feed.getEntries().iterator();
      Entry entry1 = it.next();
      Entry entry2 = it.next();
      Field field = Entry.class.getDeclaredField("finder");
      field.setAccessible(true);
      Assert.assertNotNull("First feet is not correct", field.get(entry1));
      Assert.assertEquals("Second feet is not correct", field.get(entry1), field.get(entry2));
      response.close();
   }
}
