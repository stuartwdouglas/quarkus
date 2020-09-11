package io.quarkus.rest.test.resource.param;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.resource.param.resource.ComplexPathParamExtensionResource;
import io.quarkus.rest.test.resource.param.resource.ComplexPathParamRegressionResteasy145;
import io.quarkus.rest.test.resource.param.resource.ComplexPathParamSubRes;
import io.quarkus.rest.test.resource.param.resource.ComplexPathParamSubResSecond;
import io.quarkus.rest.test.resource.param.resource.ComplexPathParamTrickyResource;
import io.quarkus.rest.test.resource.param.resource.ComplexPathParamUnlimitedResource;
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
 * @tpSubChapter Parameters
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for complex path parameters
 * @tpSince RESTEasy 3.0.16
 */
public class ComplexPathParamTest {

   public static final String WRONG_REQUEST_ERROR_MESSAGE = "Wrong content of request";

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(ComplexPathParamTest.class);
      war.addClass(PortProviderUtil.class);
      war.addClass(TestUtil.class);
      war.addClass(ComplexPathParamSubRes.class);
      war.addClass(ComplexPathParamSubResSecond.class);
      return TestUtil.finishContainerPrepare(war, null, ComplexPathParamExtensionResource.class,
            ComplexPathParamRegressionResteasy145.class, ComplexPathParamTrickyResource.class,
            ComplexPathParamUnlimitedResource.class);
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, ComplexPathParamTest.class.getSimpleName());
   }

   private void basicTest(String path, String body) {
      QuarkusRestClient client = (QuarkusRestClient)ClientBuilder.newClient();
      Response response = client.target(generateURL(path)).request().get();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      Assert.assertEquals("Wrong content of response, url may not be decoded correctly", body, response.readEntity(String.class));
      response.close();
      client.close();
   }

   /**
    * @tpTestDetails Check special characters and various path combination
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testIt() throws Exception {
      basicTest("/1,2/3/blah4-5ttt", "hello");
      basicTest("/tricky/1,2", "2Groups");
      basicTest("/tricky/h1", "prefixed");
      basicTest("/tricky/1", "hello");
      basicTest("/unlimited/1-on/and/on", "ok");
      basicTest("/repository/workspaces/aaaaaaxvi/wdddd", "sub2");
   }

}
