package io.quarkus.rest.test.providers.jaxb;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import io.quarkus.rest.test.providers.jaxb.resource.AbstractJaxbClassPerson;
import io.quarkus.rest.test.providers.jaxb.resource.AbstractJaxbClassCustomer;
import io.quarkus.rest.test.providers.jaxb.resource.AbstractJaxbClassResource;
import io.quarkus.rest.test.providers.jaxb.resource.AbstractJaxbClassCompanyCustomer;
import io.quarkus.rest.test.providers.jaxb.resource.AbstractJaxbClassPrivatCustomer;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.Assert;
import io.quarkus.rest.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.rest.test.simple.TestUtil;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class AbstractJaxbClassTest {

   static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, AbstractJaxbClassCompanyCustomer.class, AbstractJaxbClassCustomer.class,
            AbstractJaxbClassPerson.class, AbstractJaxbClassPrivatCustomer.class, AbstractJaxbClassResource.class);
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
      return PortProviderUtil.generateURL(path, AbstractJaxbClassTest.class.getSimpleName());
   }

   private static final String customerXml = "<?xml version=\"1.0\"?>\n"
            + "<abstractJaxbClassPrivatCustomer>\n"
            + "<nachname>Test</nachname>\n"
            + "<vorname>Theo</vorname>\n"
            + "<seit>2001-01-31T00:00:00+01:00</seit>\n"
            + "<adresse><plz>76133</plz><ort>Karlsruhe</ort><strasse>Moltkestrasse</strasse><hausnr>31</hausnr></adresse>\n"
            + "</abstractJaxbClassPrivatCustomer>";

   /**
    * @tpTestDetails Test for Abstract jaxb class with @XmlSeeAlso annotation
    * @tpInfo RESTEASY-126
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testPost() throws Exception {
      ResteasyWebTarget target = client.target(generateURL(""));
      String xmlInput = "<?xml version=\"1.0\"?><abstractJaxbClassPerson><name>bill</name></abstractJaxbClassPerson>";
      Response response = target.request().post(Entity.xml(xmlInput));
      Assert.assertEquals(HttpResponseCodes.SC_NO_CONTENT, response.getStatus());
      response.close();

      ResteasyWebTarget target2 = client.target(generateURL("/customer"));
      Response response2 = target2.request().post(Entity.entity(customerXml, "application/xml"));
      Assert.assertEquals(204, response2.getStatus());
      response2.close();
   }

}
