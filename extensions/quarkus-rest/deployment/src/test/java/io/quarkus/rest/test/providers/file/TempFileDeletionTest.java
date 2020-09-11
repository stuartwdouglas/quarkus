package io.quarkus.rest.test.providers.file;

import java.io.File;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.providers.file.resource.TempFileDeletionResource;
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

/**
 * @tpSubChapter File provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.1.3.Final
 */
public class TempFileDeletionTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, TempFileDeletionResource.class);
   }});

   /**
    * @tpTestDetails Resource method contains parameter of the type File. This triggers File provider, which creates
    * temporary file on the server side, which is automatically deleted in the end of the resource method invocation.
    * @tpInfo Regression test for RESTEASY-1464
    * @tpSince RESTEasy 3.1.3.Final
    */
   @Test
   public void testDeleteOnServer() throws Exception {
      Client client = ClientBuilder.newClient();
      WebTarget base = client.target(PortProviderUtil.generateURL("/test/post", TempFileDeletionTest.class.getSimpleName()));
      Response response = base.request().post(Entity.entity("hello", "text/plain"));
      Assert.assertEquals(response.getStatus(), HttpResponseCodes.SC_OK);
      String path = response.readEntity(String.class);
      File file = new File(path);
      Assert.assertFalse(file.exists());
      client.close();
   }
}
