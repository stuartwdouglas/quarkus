package io.quarkus.rest.test.client;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.client.jaxrs.ProxyBuilder;
import io.quarkus.rest.runtime.client.QuarkusRestClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import io.quarkus.rest.test.client.resource.GenericReturnTypeInterface;
import io.quarkus.rest.test.client.resource.GenericReturnTypeReader;
import io.quarkus.rest.test.client.resource.GenericReturnTypeResource;
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

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpSince RESTEasy 3.0.17
 * @tpTestCaseDetails Regression for JBEAP-4699
 */
public class GenericReturnTypeTest extends ClientTestBase{

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClasses(GenericReturnTypeInterface.class);
      return TestUtil.finishContainerPrepare(war, null, GenericReturnTypeResource.class, GenericReturnTypeReader.class);
   }});

   /**
    * @tpTestDetails Test generic type of proxy
    * @tpSince RESTEasy 3.0.17
    */
   @Test
   public void testGenericReturnType() {
      Client client = QuarkusRestClientBuilder.newClient();
      ResteasyWebTarget target = (ResteasyWebTarget) client.target(generateURL("")).register(GenericReturnTypeReader.class);
      GenericReturnTypeInterface<?> server = ProxyBuilder.builder(GenericReturnTypeInterface.class, target).build();
      Object result = server.t();
      Assert.assertEquals("abc", result);
      client.close();
   }
}
