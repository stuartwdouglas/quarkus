package io.quarkus.rest.test.providers.datasource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.providers.datasource.resource.BigSmallDataSourceResource;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import io.quarkus.rest.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.rest.test.simple.TestUtil;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;

/**
 * @tpSubChapter DataSource provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class BigSmallDataSourceTest {

   static QuarkusRestClient client;
   static final String testFilePath;

   static {
      testFilePath = TestUtil.getResourcePath(BigSmallDataSourceTest.class, "test.jpg");
   }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, BigSmallDataSourceResource.class);
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
      return PortProviderUtil.generateURL(path, BigSmallDataSourceTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Tests DataSourceProviders ability to get content type of the file attached to the request
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testPostDataSource() throws Exception {
      File file = new File(testFilePath);
      Assert.assertTrue("File " + testFilePath + " doesn't exists", file.exists());
      WebTarget target = client.target(generateURL("/jaf"));
      Response response = target.request().post(Entity.entity(file, "image/jpeg"));
      Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
      Assert.assertEquals("Unexpected content type returned from the server", "image/jpeg", response.readEntity(String.class));
   }

   /**
    * @tpTestDetails Tests DataSourceProviders ability to read and write bigger file
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testEchoDataSourceBigData() throws Exception {
      WebTarget target = client.target(generateURL("/jaf/echo"));
      File file = new File(testFilePath);
      Assert.assertTrue("File " + testFilePath + " doesn't exists", file.exists());
      Response response = target.request().post(Entity.entity(file, "image/jpeg"));
      Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());

      InputStream ris = null;
      InputStream fis = null;
      try {
         ris = response.readEntity(InputStream.class);
         fis = new FileInputStream(file);
         int fi;
         int ri;
         do {
            fi = fis.read();
            ri = ris.read();
            if (fi != ri) {
               Assert.fail("The sent and received stream is not identical.");
            }
         } while (fi != -1);
      } finally {
         if (ris != null) {
            ris.close();
         }
         if (fis != null) {
            fis.close();
         }
      }
   }

   /**
    * @tpTestDetails Tests DataSourceProviders ability to read and write small stream
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testEchoDataSourceSmallData() throws Exception {
      WebTarget target = client.target(generateURL("/jaf/echo"));
      byte[] input = "Hello World!".getBytes(StandardCharsets.UTF_8);
      Response response = target.request().post(Entity.entity(input, MediaType.APPLICATION_OCTET_STREAM));
      Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());

      InputStream ris = null;
      InputStream bis = null;
      try {
         ris = response.readEntity(InputStream.class);
         bis = new ByteArrayInputStream(input);
         int fi;
         int ri;
         do {
            fi = bis.read();
            ri = ris.read();
            if (fi != ri) {
               Assert.fail("The sent and recived stream is not identical.");
            }
         } while (fi != -1);
      } finally {
         if (ris != null) {
            ris.close();
         }
         if (bis != null) {
            bis.close();
         }
      }
   }

   /**
    * @tpTestDetails Tests DataSourceProviders ability to return InputStream for given value
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testGetDataSource() throws Exception {
      String value = "foo";
      WebTarget target = client.target(generateURL("/jaf") + "/" + value);
      Response response = target.request().get();
      Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
      Assert.assertEquals("The unexpected value returned from InputStream", value, response.readEntity(String.class));
   }
}
