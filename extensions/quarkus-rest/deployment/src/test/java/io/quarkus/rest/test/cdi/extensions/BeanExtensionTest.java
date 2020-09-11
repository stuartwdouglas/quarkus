package io.quarkus.rest.test.cdi.extensions;


import static org.junit.Assert.assertEquals;

import javax.enterprise.inject.spi.Extension;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.cdi.extensions.resource.CDIExtensionsBoston;
import io.quarkus.rest.test.cdi.extensions.resource.CDIExtensionsBostonBean;
import io.quarkus.rest.test.cdi.extensions.resource.CDIExtensionsBostonBeanExtension;
import io.quarkus.rest.test.cdi.extensions.resource.CDIExtensionsBostonHolder;
import io.quarkus.rest.test.cdi.extensions.resource.CDIExtensionsBostonlLeaf;
import io.quarkus.rest.test.cdi.extensions.resource.CDIExtensionsTestReader;
import io.quarkus.rest.test.cdi.extensions.resource.CDIExtensionsResource;
import io.quarkus.rest.test.cdi.util.Utilities;
import io.quarkus.rest.test.cdi.util.UtilityProducer;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import io.quarkus.rest.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.rest.test.simple.TestUtil;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test CDI extensions for bean.
 * BostonBeanExtension implements a CDI extension, it creates a BostonBean for each of the two classes,
 * BostonHolder and BostonLeaf, that are annotated with @Boston, and it registers them with the CDI runtime.
 * @tpSince RESTEasy 3.0.16
 */
public class BeanExtensionTest {
   protected static final Logger log = LogManager.getLogger(BeanExtensionTest.class.getName());

   @SuppressWarnings(value = "unchecked")
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClasses(UtilityProducer.class, Utilities.class)
            .addClasses(CDIExtensionsBostonBeanExtension.class, CDIExtensionsBoston.class, CDIExtensionsBostonBean.class)
            .addClasses(CDIExtensionsResource.class, CDIExtensionsTestReader.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsServiceProvider(Extension.class, CDIExtensionsBostonBeanExtension.class);

      JavaArchive jar = ShrinkWrap.create(JavaArchive.class).addClasses(CDIExtensionsBostonHolder.class, CDIExtensionsBostonlLeaf.class);
      war.addAsLibrary(jar);

      return TestUtil.finishContainerPrepare(war, null, (Class<?>[]) null);
   }});

   /**
    * @tpTestDetails Client get request. Resource check extension bean on server.
    * @tpPassCrit Response status should not contain error.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testBostonBeans() throws Exception {
      log.info("starting testBostonBeans()");

      Client client = ClientBuilder.newClient();
      WebTarget base = client.target(PortProviderUtil.generateURL("/extension/boston/", BeanExtensionTest.class.getSimpleName()));
      Response response = base.request().post(Entity.text(new String()));

      log.info("Response status: " + response.getStatus());

      assertEquals(HttpResponseCodes.SC_OK, response.getStatus());

      response.close();
      client.close();
   }
}
