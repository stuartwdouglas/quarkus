package io.quarkus.rest.test.cdi.basic;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.cdi.basic.resource.OutOfBandResource;
import io.quarkus.rest.test.cdi.basic.resource.OutOfBandResourceIntf;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
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

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1049.
 * @tpSince RESTEasy 3.0.16
 */
public class OutOfBandTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return war;
   }});

   /**
    * @tpTestDetails JAX-RS resource methods can be called outside the context of a servlet request, leading to NPEs.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testTimerInterceptor() throws Exception {
      Client client = ClientBuilder.newClient();

      // Schedule timer.
      WebTarget base = client.target(PortProviderUtil.generateURL("/timer/schedule", "RESTEASY-1008"));
      Response response = base.request().get();
      assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      response.close();

      // Verify timer expired and timer interceptor was executed.
      base = client.target(PortProviderUtil.generateURL("/timer/test", "RESTEASY-1008"));
      response = base.request().get();
      assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      response.close();

      client.close();
   }
}
