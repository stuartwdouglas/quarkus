package io.quarkus.rest.test.cdi.inheritence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.cdi.inheritence.resource.CDIInheritenceBook;
import io.quarkus.rest.test.cdi.inheritence.resource.CDIInheritenceBookVanillaAlternative;
import io.quarkus.rest.test.cdi.inheritence.resource.CDIInheritenceInheritanceResource;
import io.quarkus.rest.test.cdi.inheritence.resource.CDIInheritenceSelectBook;
import io.quarkus.rest.test.cdi.inheritence.resource.CDIInheritenceStereotypeAlternative;
import io.quarkus.rest.test.cdi.util.UtilityProducer;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import io.quarkus.rest.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.rest.test.simple.TestUtil;


import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails This class tests CDI inheritance (BookVanilaAlternative should not be used)
 * @tpSince RESTEasy 3.0.16
 */
public class AlternativeVanillaInheritanceTest {
   protected static final Logger log = LogManager.getLogger(AlternativeVanillaInheritanceTest.class.getName());

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClasses(UtilityProducer.class)
            .addClasses(CDIInheritenceBook.class, CDIInheritenceSelectBook.class, CDIInheritenceStereotypeAlternative.class, CDIInheritenceBookVanillaAlternative.class, CDIInheritenceInheritanceResource.class)
            .addAsWebInfResource(SpecializedInheritanceTest.class.getPackage(), "alternativeVanillaBeans.xml", "beans.xml");
      return TestUtil.finishContainerPrepare(war, null, (Class<?>[]) null);
   }});

   /**
    * @tpTestDetails Client get request. Resource check inheritance bean on server.
    * @tpPassCrit Response status should not contain error.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testAlternative() throws Exception {
      Client client = ClientBuilder.newClient();
      log.info("starting testAlternative()");
      WebTarget base = client.target(PortProviderUtil.generateURL("/alternative/vanilla", AlternativeVanillaInheritanceTest.class.getSimpleName()));
      Response response = base.request().get();
      log.info("Status: " + response.getStatus());
      assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      response.close();
      client.close();
   }
}
