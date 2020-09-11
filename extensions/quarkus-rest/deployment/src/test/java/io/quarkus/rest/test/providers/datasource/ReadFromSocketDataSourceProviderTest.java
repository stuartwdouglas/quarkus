package io.quarkus.rest.test.providers.datasource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.ws.rs.core.MediaType;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.providers.datasource.resource.ReadFromSocketDataSourceProviderResource;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.resteasy.plugins.providers.DataSourceProvider;
import org.junit.After;
import org.junit.AfterClass;
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

/**
 * @tpSubChapter DataSource provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class ReadFromSocketDataSourceProviderTest {

   protected static final Logger logger = Logger.getLogger(ReadFromSocketDataSourceProviderTest.class.getName());
   static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      return TestUtil.finishContainerPrepare(war, null, ReadFromSocketDataSourceProviderResource.class);
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
      return PortProviderUtil.generateURL(path, ReadFromSocketDataSourceProviderTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Tests DataSourceProviders ability to read input stream entirely, using socket buffer for reading
    * @tpInfo RESTEASY-779
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testReadFromSocketDataSourceProvider() throws Exception {
      // important - see https://issues.jboss.org/browse/RESTEASY-779
      ConnectionConfig connConfig = ConnectionConfig.custom().setBufferSize((ReadFromSocketDataSourceProviderResource.KBs - 1) * 1024)
            .build();
      CloseableHttpClient client = HttpClients.custom().setDefaultConnectionConfig(connConfig).build();
      HttpGet httpGet = new HttpGet(generateURL("/"));
      CloseableHttpResponse response = client.execute(httpGet);
      InputStream inputStream = null;
      try {
         inputStream = response.getEntity().getContent();
         DataSourceProvider.readDataSource(inputStream, MediaType.TEXT_PLAIN_TYPE);
         Assert.assertEquals("The input stream was not read entirely", 0, findSizeOfRemainingDataInStream(inputStream));
      } finally {
         IOUtils.closeQuietly(inputStream);
      }
   }

   static int countTempFiles() throws Exception {
      String tmpdir = System.getProperty("java.io.tmpdir");
      File dir = new File(tmpdir);
      int counter = 0;
      for (File file : dir.listFiles()) {
         if (file.getName().startsWith("resteasy-provider-datasource")) {
            counter++;
         }
      }
      return counter;
   }

   private int findSizeOfRemainingDataInStream(InputStream inputStream) throws IOException {
      byte[] buf = new byte[4 * 1024];
      int bytesRead, totalBytesRead = 0;
      while ((bytesRead = inputStream.read(buf, 0, buf.length)) != -1) {
         totalBytesRead += bytesRead;
      }
      return totalBytesRead;
   }

   @AfterClass
   public static void afterClass() {
      String tmpdir = System.getProperty("java.io.tmpdir");
      File dir = new File(tmpdir);
      for (File file : dir.listFiles()) {
         if (file.getName().startsWith("resteasy-provider-datasource")) {
            file.delete();
         }
      }
   }


}
