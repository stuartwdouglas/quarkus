package io.quarkus.rest.test.core.interceptors;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.core.interceptors.resource.PreProcessorExceptionMapperCandlepinException;
import io.quarkus.rest.test.core.interceptors.resource.PreProcessorExceptionMapperCandlepinUnauthorizedException;
import io.quarkus.rest.test.core.interceptors.resource.PreProcessorExceptionMapperPreProcessSecurityInterceptor;
import io.quarkus.rest.test.core.interceptors.resource.PreProcessorExceptionMapperResource;
import io.quarkus.rest.test.core.interceptors.resource.PreProcessorExceptionMapperRuntimeExceptionMapper;
import org.jboss.resteasy.spi.HttpResponseCodes;
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

import javax.ws.rs.core.Response;

import static org.jboss.resteasy.utils.PortProviderUtil.generateURL;

/**
 * @tpSubChapter Interceptors
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-433
 * @tpSince RESTEasy 3.0.16
 */
public class PreProcessorExceptionMapperTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(PreProcessorExceptionMapperCandlepinException.class);
      war.addClass(PreProcessorExceptionMapperCandlepinUnauthorizedException.class);
      return TestUtil.finishContainerPrepare(war, null, PreProcessorExceptionMapperPreProcessSecurityInterceptor.class,
                                                PreProcessorExceptionMapperRuntimeExceptionMapper.class,
                                                PreProcessorExceptionMapperResource.class);
   }});

   /**
    * @tpTestDetails Generate PreProcessorExceptionMapperCandlepinUnauthorizedException
    * @tpPassCrit SC_PRECONDITION_FAILED (412) HTTP code is excepted
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testMapper() throws Exception {
      QuarkusRestClient client = (QuarkusRestClient)ClientBuilder.newClient();
      Response response = client.target(generateURL("/interception", GzipTest.class.getSimpleName())).request().get();
      Assert.assertEquals(HttpResponseCodes.SC_PRECONDITION_FAILED, response.getStatus());
      response.close();
      client.close();
   }

}
