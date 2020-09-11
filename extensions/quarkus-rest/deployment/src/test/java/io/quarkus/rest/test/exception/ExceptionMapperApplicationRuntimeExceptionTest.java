package io.quarkus.rest.test.exception;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import io.quarkus.rest.runtime.client.QuarkusRestClient;
import javax.ws.rs.client.ClientBuilder;
import io.quarkus.rest.test.exception.resource.ExceptionMapperApplicationRuntimeCustomMapper;
import io.quarkus.rest.test.exception.resource.ExceptionMapperApplicationRuntimeMapper;
import io.quarkus.rest.test.exception.resource.ExceptionMapperApplicationRuntimeResource;
import io.quarkus.rest.test.exception.resource.ExceptionMapperCustomRuntimeException;
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
import javax.ws.rs.core.Response;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 * @tpTestCaseDetails Regression test for RESTEASY-421
 */
public class ExceptionMapperApplicationRuntimeExceptionTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

      war.addClass(ExceptionMapperCustomRuntimeException.class);
      return TestUtil.finishContainerPrepare(war, null, ExceptionMapperApplicationRuntimeCustomMapper.class,
            ExceptionMapperApplicationRuntimeMapper.class, ExceptionMapperApplicationRuntimeResource.class);
   }});

   /**
    * @tpTestDetails Check ExceptionMapper for ApplicationException
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testMapper() throws Exception {
      QuarkusRestClient client = (QuarkusRestClient)ClientBuilder.newClient();
      WebTarget base = client.target(PortProviderUtil.generateURL("/test", ExceptionMapperApplicationRuntimeExceptionTest.class.getSimpleName()));
      Response response = base.request().get();

      Assert.assertEquals(Response.Status.PRECONDITION_FAILED.getStatusCode(), response.getStatus());

      response.close();
      client.close();
   }

}
