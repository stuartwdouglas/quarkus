package io.quarkus.rest.test.asynch;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.test.asynch.resource.AsyncGenericEntityMessageBodyWriter;
import io.quarkus.rest.test.asynch.resource.AsyncGenericEntityResource;
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
 * @tpSubChapter Asynchronous RESTEasy
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test getting GenericType from return entity.
 * @tpSince RESTEasy 3.7.0
 */
public class AsyncGenericEntityTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null,
            AsyncGenericEntityMessageBodyWriter.class,
            AsyncGenericEntityResource.class);
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, AsyncGenericEntityTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Test getting GenericType from return entity.
    * @tpSince RESTEasy 3.7.0
    */
   @Test
   public void testCalls() {
       Client client = ClientBuilder.newClient();
       Builder request = client.target(generateURL("/test")).request();
       Response response = request.get();
       Assert.assertEquals(200, response.getStatus());
       Assert.assertEquals("ok", response.readEntity(String.class));
       response.close();
       client.close();
   }

}
