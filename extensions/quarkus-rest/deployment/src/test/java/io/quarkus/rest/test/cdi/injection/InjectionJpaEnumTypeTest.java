package io.quarkus.rest.test.cdi.injection;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.cdi.injection.resource.ApplicationUser;
import io.quarkus.rest.test.cdi.injection.resource.UserManager;
import io.quarkus.rest.test.cdi.injection.resource.UserRepository;
import io.quarkus.rest.test.cdi.injection.resource.UserResource;
import io.quarkus.rest.test.cdi.injection.resource.UserType;
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

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;


/**
 * @tpSubChapter Injection
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test based on reproducer from WFLY-7037. Enum type UserType is attribute of JPA entity ApplicationUser
 * and this attribute is returned via GET request. Note this is not a regression test since in order to reproduce the issue
 * described in WFLY-7037 is needed to examine the heapdump.
 * @tpSince RESTEasy 3.1.0
 */
public class InjectionJpaEnumTypeTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClasses(UserManager.class, UserRepository.class, UserResource.class,
            UserType.class, ApplicationUser.class);
      war.addAsResource(InjectionJpaEnumTypeTest.class.getPackage(), "injectionJpaEnumType/persistence.xml", "META-INF/persistence.xml");
      war.addAsResource(InjectionJpaEnumTypeTest.class.getPackage(), "injectionJpaEnumType/create.sql", "META-INF/create.sql");
      war.addAsResource(InjectionJpaEnumTypeTest.class.getPackage(), "injectionJpaEnumType/load.sql", "META-INF/load.sql");
      return TestUtil.finishContainerPrepare(war, null, (Class<?>[]) null);
   }});

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, InjectionJpaEnumTypeTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Retrieves attribute UserType from the datasource in json format
    * @tpSince RESTEasy 3.1.0
    */
   @Test
   public void testEnumJackson() throws Exception {
      QuarkusRestClient client = (QuarkusRestClient)ClientBuilder.newClient();
      WebTarget base = client.target(generateURL("/user"));
      String val = base.request().accept(MediaType.APPLICATION_JSON_TYPE).get().readEntity(String.class);
      Assert.assertEquals("{\"id\":1,\"userType\":\"TYPE_ONE\"}", val);
      client.close();
   }

   /**
    * @tpTestDetails Retrieves attribute UserType from the datasource in xml format
    * @tpSince RESTEasy 3.1.0
    */
   @Test
   public void testEnumJaxb() throws Exception {
      QuarkusRestClient client = (QuarkusRestClient)ClientBuilder.newClient();
      WebTarget base = client.target(generateURL("/user"));
      String val = base.request().accept(MediaType.APPLICATION_XML_TYPE).get().readEntity(String.class);
      Assert.assertTrue(val.contains("<applicationUser><id>1</id><userType>TYPE_ONE</userType></applicationUser>"));
      client.close();
   }
}
