package io.quarkus.rest.test.providers.jackson2;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import io.quarkus.rest.test.core.basic.resource.ApplicationTestScannedApplication;
import io.quarkus.rest.test.providers.jackson2.resource.JacksonDatatypeEndPoint;
import io.quarkus.rest.test.providers.jackson2.resource.JacksonDatatypeJacksonProducer;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
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

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;

import java.util.HashMap;
import java.util.Map;

/**
 * @tpSubChapter Jackson2 provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for WFLY-5916. Integration tests for jackson-datatype-jsr310 and jackson-datatype-jdk8 modules
 * @tpSince RESTEasy 3.1.0.CR3
 */
public class JacksonDatatypeTest {
   private static final String DEFAULT_DEPLOYMENT = String.format("%sDefault",
         JacksonDatatypeTest.class.getSimpleName());
   private static final String DEPLOYMENT_WITH_DATATYPE = String.format("%sWithDatatypeSupport",
         JacksonDatatypeTest.class.getSimpleName());

   static QuarkusRestClient client;
   protected static final Logger logger = Logger.getLogger(JacksonDatatypeTest.class.getName());

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

      Map<String, String> contextParam = new HashMap<>();
      contextParam.put(ResteasyContextParameters.RESTEASY_PREFER_JACKSON_OVER_JSONB, "true");
      return TestUtil.finishContainerPrepare(war, contextParam, ApplicationTestScannedApplication.class,
              JacksonDatatypeEndPoint.class);
   }});

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      Map<String, String> contextParam = new HashMap<>();
      contextParam.put(ResteasyContextParameters.RESTEASY_PREFER_JACKSON_OVER_JSONB, "true");
      return TestUtil.finishContainerPrepare(war, contextParam, JacksonDatatypeEndPoint.class,
            JacksonDatatypeJacksonProducer.class, ApplicationTestScannedApplication.class);
   }});

   private String requestHelper(String endPath, String deployment) {
      String url = PortProviderUtil.generateURL(String.format("/scanned/%s", endPath), deployment);
      WebTarget base = client.target(url);
      Response response = base.request().get();
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      String strResponse = response.readEntity(String.class);
      logger.info(String.format("Url: %s", url));
      logger.info(String.format("Response: %s", strResponse));
      return strResponse;
   }

   /**
    * @tpTestDetails Check string type without datatype supported
    * @tpSince RESTEasy 3.1.0.CR3
    */
   @Test
   public void testDatatypeNotSupportedString() throws Exception {
      String strResponse = requestHelper("string", DEFAULT_DEPLOYMENT);
      Assert.assertThat("Wrong conversion of String", strResponse, containsString("someString"));
   }

   /**
    * @tpTestDetails Check date type without datatype supported
    * @tpSince RESTEasy 3.1.0.CR3
    */
   @Test
   public void testDatatypeNotSupportedDate() throws Exception {
      String strResponse = requestHelper("date", DEFAULT_DEPLOYMENT);
      Assert.assertThat("Wrong conversion of Date", strResponse.matches("^[0-9]*$"), is(true));
   }

   /**
    * @tpTestDetails Check duration type without datatype supported
    * @tpSince RESTEasy 3.1.0.CR3
    */
   @Test
   public void testDatatypeNotSupportedDuration() throws Exception {
      String strResponse = requestHelper("duration", DEFAULT_DEPLOYMENT);
      Assert.assertThat("Wrong conversion of Duration", strResponse, not(containsString("PT5.000000006S")));
   }

   /**
    * @tpTestDetails Check null optional type without datatype supported
    * @tpSince RESTEasy 3.1.0.CR3
    */
   @Test
   public void testDatatypeNotSupportedOptionalNull() throws Exception {
      String strResponse = requestHelper("optional/true", DEFAULT_DEPLOYMENT);
      Assert.assertThat("Wrong conversion of Optional (null)", strResponse, not(containsString("null")));
   }

   /**
    * @tpTestDetails Check not null optional type without datatype supported
    * @tpSince RESTEasy 3.1.0.CR3
    */
   @Test
   public void testDatatypeNotSupportedOptionalNotNull() throws Exception {
      String strResponse = requestHelper("optional/false", DEFAULT_DEPLOYMENT);
      Assert.assertThat("Wrong conversion of Optional (not null)", strResponse, not(containsString("info@example.com")));
   }

   /**
    * @tpTestDetails Check string type with datatype supported
    * @tpSince RESTEasy 3.1.0.CR3
    */
   @Test
   public void testDatatypeSupportedString() throws Exception {
      String strResponse = requestHelper("string", DEPLOYMENT_WITH_DATATYPE);
      Assert.assertThat("Wrong conversion of String", strResponse, containsString("someString"));
   }

   /**
    * @tpTestDetails Check date type with datatype supported
    * @tpSince RESTEasy 3.1.0.CR3
    */
   @Test
   public void testDatatypeSupportedDate() throws Exception {
      String strResponse = requestHelper("date", DEPLOYMENT_WITH_DATATYPE);
      Assert.assertThat("Wrong conversion of Date", strResponse.matches("^[0-9]*$"), is(false));
   }

   /**
    * @tpTestDetails Check duration type with datatype supported
    * @tpSince RESTEasy 3.1.0.CR3
    */
   @Test
   public void testDatatypeSupportedDuration() throws Exception {
      String strResponse = requestHelper("duration", DEPLOYMENT_WITH_DATATYPE);
      Assert.assertThat("Wrong conversion of Duration", strResponse, containsString("5.000000006"));
   }

   /**
    * @tpTestDetails Check null optional type with datatype supported
    * @tpSince RESTEasy 3.1.0.CR3
    */
   @Test
   public void testDatatypeSupportedOptionalNull() throws Exception {
      String strResponse = requestHelper("optional/true", DEPLOYMENT_WITH_DATATYPE);
      Assert.assertThat("Wrong conversion of Optional (null)", strResponse, containsString("null"));
   }

   /**
    * @tpTestDetails Check not null optional type with datatype supported
    * @tpSince RESTEasy 3.1.0.CR3
    */
   @Test
   public void testDatatypeSupportedOptionalNotNull() throws Exception {
      String strResponse = requestHelper("optional/false", DEPLOYMENT_WITH_DATATYPE);
      Assert.assertThat("Wrong conversion of Optional (not null)", strResponse, containsString("info@example.com"));
   }
}
