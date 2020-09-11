package io.quarkus.rest.test.client.proxy;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import io.quarkus.rest.test.client.proxy.resource.GenericEntities.EntityExtendingBaseEntity;
import io.quarkus.rest.test.client.proxy.resource.GenericEntities.GenericEntityExtendingBaseEntityProxy;
import io.quarkus.rest.test.client.proxy.resource.GenericEntities.GenericEntityExtendingBaseEntityResource;
import io.quarkus.rest.test.client.proxy.resource.GenericEntities.MultipleGenericEntities;
import io.quarkus.rest.test.client.proxy.resource.GenericEntities.MultipleGenericEntitiesProxy;
import io.quarkus.rest.test.client.proxy.resource.GenericEntities.MultipleGenericEntitiesResource;
import io.quarkus.rest.test.client.proxy.resource.GenericProxyBase;
import io.quarkus.rest.test.client.proxy.resource.GenericProxySpecificProxy;
import io.quarkus.rest.test.client.proxy.resource.GenericProxyResource;
import org.jboss.resteasy.spi.HttpResponseCodes;
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

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;

import static io.quarkus.rest.test.client.proxy.resource.GenericEntities.GenericEntityExtendingBaseEntityResource.FIRST_NAME;
import static io.quarkus.rest.test.client.proxy.resource.GenericEntities.GenericEntityExtendingBaseEntityResource.LAST_NAME;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1047.
 * @tpSince RESTEasy 3.0.16
 */
public class GenericProxyTest {
   private static QuarkusRestClient client;

   @BeforeClass
   public static void before() throws Exception {
      client = (QuarkusRestClient)ClientBuilder.newClient();
   }

   @AfterClass
   public static void after() throws Exception {
      client.close();
   }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClasses(GenericProxyBase.class, GenericProxySpecificProxy.class);
      war.addPackage(MultipleGenericEntities.class.getPackage());
      return TestUtil.finishContainerPrepare(war, null, GenericProxyResource.class,
              GenericEntityExtendingBaseEntityResource.class,
              MultipleGenericEntitiesResource.class);
   }});

   private static String generateBaseUrl() {
      return PortProviderUtil.generateBaseUrl(GenericProxyTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Test generic proxy in client.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testEcho() {
      ResteasyWebTarget target = client.target(generateBaseUrl());
      GenericProxySpecificProxy proxy = target.proxy(GenericProxySpecificProxy.class);
      String hello = proxy.sayHi("hello");
      Assert.assertEquals("Response has wrong content", "hello", hello);
      hello = proxy.sayHi("hello123");
      Assert.assertEquals("Response has wrong content", "hello123", hello);
   }

   /**
    * @tpTestDetails Test generic proxy in client.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testEchoNoProxy() {
      ResteasyWebTarget target = client.target(generateBaseUrl() + "/say/hello");
      Response response = target.request().post(Entity.text("hello"));

      String hello = response.readEntity(String.class);
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      Assert.assertEquals("Response has wrong content", "hello", hello);

      response.close();
   }

   /**
    * @tpTestDetails Test generic proxy in client extending another interface. Test for RESTEASY-1432.
    * @tpSince RESTEasy 3.7.0
    */
   @Test
   public void testInterfaceWithGenericTypeWithClientProxy() {
      GenericEntityExtendingBaseEntityProxy proxy = client.target(generateBaseUrl()).proxy(GenericEntityExtendingBaseEntityProxy.class);
      EntityExtendingBaseEntity entity;

      entity = proxy.findOne();
      Assert.assertEquals(entity.getLastName(), LAST_NAME);

      List<EntityExtendingBaseEntity> entities = proxy.findAll();
      Assert.assertEquals(entities.get(0).getLastName(), LAST_NAME);


      MultipleGenericEntitiesProxy proxy1 = client.target(generateBaseUrl()).proxy(MultipleGenericEntitiesProxy.class);

      HashMap<String, EntityExtendingBaseEntity> hashMap = proxy1.findHashMap();
      entity = hashMap.get(FIRST_NAME);
      Assert.assertEquals(entity.getLastName(), LAST_NAME);
   }

}
