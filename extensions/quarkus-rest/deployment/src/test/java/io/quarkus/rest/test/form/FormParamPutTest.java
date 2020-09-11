package io.quarkus.rest.test.form;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.form.resource.FormParamPutResource;
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
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

/**
 * @tpSubChapter Form tests
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for JBEAP-982
 * @tpSince RESTEasy 3.0.16
 */
public class FormParamPutTest {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, FormParamPutResource.class);
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, FormParamPutTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Test with query param and without query param
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void test1() throws Exception {
      Client client = ClientBuilder.newClient();
      WebTarget put = client.target(generateURL("/test/42?foo=xyz"));

      Form form = new Form().param("formParam", "Weinan Li");
      Response response = put.request().put(Entity.form(form));
      response.close();

      WebTarget get = client.target(generateURL("/test"));
      assertEquals("Weinan Li", get.request().get().readEntity(String.class));
      client.close();
   }
}
