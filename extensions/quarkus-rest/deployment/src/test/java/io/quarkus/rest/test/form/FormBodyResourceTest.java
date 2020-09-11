package io.quarkus.rest.test.form;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.form.resource.FormBodyResourceClient;
import io.quarkus.rest.test.form.resource.FormBodyResourceForm;
import io.quarkus.rest.test.form.resource.FormBodyResourceResource;
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

/**
 * @tpSubChapter Form tests
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class FormBodyResourceTest {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClasses(FormBodyResourceClient.class);
      war.addClasses(FormBodyResourceForm.class);
      return TestUtil.finishContainerPrepare(war, null, FormBodyResourceResource.class);
   }});

   /**
    * @tpTestDetails Check body of form.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void test() {
      QuarkusRestClient client = (QuarkusRestClient)ClientBuilder.newClient();
      FormBodyResourceClient proxy = client.target(
            PortProviderUtil.generateBaseUrl(FormParameterTest.class.getSimpleName()))
            .proxyBuilder(FormBodyResourceClient.class).build();
      Assert.assertEquals("foo.gotIt", proxy.put("foo"));
      client.close();
   }
}
