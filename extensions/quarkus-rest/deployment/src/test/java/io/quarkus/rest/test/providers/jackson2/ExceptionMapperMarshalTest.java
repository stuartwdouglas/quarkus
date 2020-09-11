package io.quarkus.rest.test.providers.jackson2;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import io.quarkus.rest.test.providers.jackson2.resource.ExceptionMapperIOExceptionMapper;
import io.quarkus.rest.test.providers.jackson2.resource.ExceptionMapperMarshalErrorMessage;
import io.quarkus.rest.test.providers.jackson2.resource.ExceptionMapperMarshalMyCustomException;
import io.quarkus.rest.test.providers.jackson2.resource.ExceptionMapperMarshalName;
import io.quarkus.rest.test.providers.jackson2.resource.ExceptionMapperMarshalMyCustomExceptionMapper;
import io.quarkus.rest.test.providers.jackson2.resource.ExceptionMapperMarshalResource;
import io.quarkus.rest.test.providers.jackson2.resource.MyEntity;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.spi.util.Types;
import org.jboss.resteasy.utils.PermissionUtil;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.Assert;
import io.quarkus.rest.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.rest.test.simple.TestUtil;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.lang.reflect.ReflectPermission;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PropertyPermission;

/**
 * @tpSubChapter Jackson2 provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-937
 * @tpSince RESTEasy 3.0.16
 */
public class ExceptionMapperMarshalTest {

   protected static final Logger logger = Logger.getLogger(ProxyWithGenericReturnTypeJacksonTest.class.getName());
   static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(Jackson2Test.class);
      war.addAsManifestResource(PermissionUtil.createPermissionsXmlAsset(
            new RuntimePermission("getProtectionDomain"),
            new ReflectPermission("suppressAccessChecks"),
            new PropertyPermission("resteasy.server.tracing.*", "read")
      ), "permissions.xml");
      Map<String, String> contextParam = new HashMap<>();
      contextParam.put(ResteasyContextParameters.RESTEASY_PREFER_JACKSON_OVER_JSONB, "true");
      return TestUtil.finishContainerPrepare(war, contextParam, ExceptionMapperMarshalErrorMessage.class, ExceptionMapperMarshalMyCustomException.class,
            MyEntity.class, ExceptionMapperIOExceptionMapper.class,
            ExceptionMapperMarshalMyCustomExceptionMapper.class, ExceptionMapperMarshalName.class, ExceptionMapperMarshalResource.class);
   }});

   @Before
   public void init() {
      client = (QuarkusRestClient)ClientBuilder.newClient();
   }

   @After
   public void after() throws Exception {
      client.close();
   }

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, ProxyWithGenericReturnTypeJacksonTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Tests usage of custom ExceptionMapper producing json response
    * @tpPassCrit The resource returns Success response
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testCustomUsed() {
      Type exceptionType = Types.getActualTypeArgumentsOfAnInterface(ExceptionMapperMarshalMyCustomExceptionMapper.class, ExceptionMapper.class)[0];
      Assert.assertEquals(ExceptionMapperMarshalMyCustomException.class, exceptionType);

      Response response = client.target(generateURL("/resource/custom")).request().get();
      Assert.assertEquals(response.getStatus(), HttpResponseCodes.SC_OK);
      List<ExceptionMapperMarshalErrorMessage> errors = response.readEntity(new GenericType<List<ExceptionMapperMarshalErrorMessage>>() {
      });
      Assert.assertEquals("The response has unexpected content", "error", errors.get(0).getError());
   }

   @Test
   public void testMyCustomUsed() {
      Response response = client.target(generateURL("/resource/customME")).request().get();
      String text = response.readEntity(String.class);

      Assert.assertEquals(response.getStatus(), HttpResponseCodes.SC_OK);
      Assert.assertTrue("Response does not contain UN_KNOWN_ERR", text.contains("UN_KNOWN_ERR"));
   }
}
