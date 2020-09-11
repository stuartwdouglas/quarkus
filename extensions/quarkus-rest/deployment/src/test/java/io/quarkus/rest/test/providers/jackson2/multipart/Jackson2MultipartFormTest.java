package io.quarkus.rest.test.providers.jackson2.multipart;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import io.quarkus.rest.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.rest.test.simple.TestUtil;

public class Jackson2MultipartFormTest {

   static QuarkusRestClient client;

   @BeforeClass
   public static void init() {
      client = (QuarkusRestClient)ClientBuilder.newClient();
   }

   @AfterClass
   public static void close() {
      client.close();
   }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(Jackson2MultipartFormTest.class);
      return TestUtil.finishContainerPrepare(war, null, JsonFormResource.class, JsonUser.class);
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, Jackson2MultipartFormTest.class.getSimpleName());
   }

   @Test
   public void testJacksonProxy() {
      JsonForm proxy = client.target(generateURL("")).proxy(JsonForm.class);
      String name = proxy.putMultipartForm(new JsonFormResource.Form(new JsonUser("bill")));
      Assert.assertEquals("bill", name);
   }
}
