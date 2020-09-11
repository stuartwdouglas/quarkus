package io.quarkus.rest.test.client.proxy;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import io.quarkus.rest.test.client.proxy.resource.ContentLanguageInterface;
import io.quarkus.rest.test.client.proxy.resource.ContentLanguageResource;
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

//Test for RESTEASY-1537:Client proxy framework clears previously set Content-Language header when setting POST message body entity
public class ContentLanguageProxyTest
{
   private static QuarkusRestClient client;

   @BeforeClass
   public static void before() throws Exception
   {
      client = (QuarkusRestClient)ClientBuilder.newClient();
   }

   @AfterClass
   public static void after() throws Exception
   {
      client.close();
   }

   @Deployment
   public static Archive<?> deployUriInfoSimpleResource()
   {
      WebArchive war = TestUtil.prepareArchive(ContentLanguageProxyTest.class.getSimpleName());
      war.addClasses(ContentLanguageInterface.class);
      return TestUtil.finishContainerPrepare(war, null, ContentLanguageResource.class);
   }

   private static String generateBaseUrl()
   {
      return PortProviderUtil.generateBaseUrl(ContentLanguageProxyTest.class.getSimpleName());
   }

   @Test
   public void testProxy() throws Exception
   {
      ResteasyWebTarget target = client.target(generateBaseUrl());
      ContentLanguageInterface proxy = target.proxy(ContentLanguageInterface.class);
      String contentLangFirst = proxy.contentLang1("fr", "subject");
      Assert.assertEquals("Content_Language header is expected set in request", "frsubject", contentLangFirst);
      String contentLangSecond = proxy.contentLang2("subject", "fr");
      Assert.assertEquals("Content_Language header is expected set in request", "subjectfr", contentLangSecond);
   }
}
