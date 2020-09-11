package io.quarkus.rest.test.cdi.basic;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.cdi.basic.resource.BeanReaderWriterConfigBean;
import io.quarkus.rest.test.cdi.basic.resource.BeanReaderWriterService;
import io.quarkus.rest.test.cdi.basic.resource.BeanReaderWriterXFormat;
import io.quarkus.rest.test.cdi.basic.resource.BeanReaderWriterXFormatProvider;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
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
import javax.ws.rs.core.Response;


/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for custom reader-writer for bean.
 * @tpSince RESTEasy 3.0.16
 */
public class BeanReaderWriterTest {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClasses(BeanReaderWriterConfigBean.class,
            BeanReaderWriterService.class, BeanReaderWriterXFormat.class, BeanReaderWriterXFormatProvider.class);

      war.addAsWebInfResource(BeanReaderWriterTest.class.getPackage(), "BeanReaderWriterBeans.xml", "beans.xml");

      return war;
   }});

   /**
    * @tpTestDetails Bean set constant used in custom reader-writer.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testIt2() throws Exception {
      Client client = ClientBuilder.newClient();
      Response response = client.target(PortProviderUtil.generateBaseUrl(BeanReaderWriterTest.class.getSimpleName())).request().get();
      String format = response.readEntity(String.class);
      Assert.assertEquals("foo 1.1", format);
      response.close();
      client.close();
   }
}

