package io.quarkus.rest.test.resource.param;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.resource.param.resource.QueryParamWithMultipleEqualsResource;
import org.jboss.resteasy.spi.HttpResponseCodes;
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
import javax.ws.rs.core.Response;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 * @tpTestCaseDetails Test query params with multiple equals
 */
public class QueryParamWithMultipleEqualsTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, QueryParamWithMultipleEqualsResource.class);
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, QueryParamWithMultipleEqualsTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Test query parameter "foo=weird=but=valid"
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testQueryParam() throws Exception {
      QuarkusRestClient client = (QuarkusRestClient)ClientBuilder.newClient();
      Response response = client.target(generateURL("/test?foo=weird=but=valid")).request().get();

      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      String entity = response.readEntity(String.class);
      Assert.assertEquals("Wrong content of response", "weird=but=valid", entity);

      response.close();
      client.close();
   }
}
