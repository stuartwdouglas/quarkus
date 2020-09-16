package io.quarkus.rest.test.exception;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.exception.resource.ExceptionMapperCustomRuntimeException;
import io.quarkus.rest.test.exception.resource.ExceptionMapperInjectionCustomMapper;
import io.quarkus.rest.test.exception.resource.ExceptionMapperInjectionCustomSimpleMapper;
import io.quarkus.rest.test.exception.resource.ExceptionMapperInjectionException;
import io.quarkus.rest.test.exception.resource.ExceptionMapperInjectionNotFoundMapper;
import io.quarkus.rest.test.exception.resource.ExceptionMapperInjectionResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 * @tpTestCaseDetails ExceptionMapper testing. Regression test for RESTEASY-300 and RESTEASY-396
 */
public class ExceptionMapperInjectionTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(ExceptionMapperCustomRuntimeException.class);
                    war.addClass(ExceptionMapperInjectionException.class);
                    return TestUtil.finishContainerPrepare(war, null, ExceptionMapperInjectionCustomMapper.class,
                            ExceptionMapperInjectionCustomSimpleMapper.class, ExceptionMapperInjectionNotFoundMapper.class,
                            ExceptionMapperInjectionResource.class);
                }
            });

    @BeforeClass
    public static void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterClass
    public static void after() throws Exception {
        client.close();
    }

    public String generateUrl(String path) {
        return PortProviderUtil.generateURL(path, ExceptionMapperInjectionTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Check non-existent path
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testNotFound() throws Exception {
        WebTarget base = client.target(generateUrl("/test/nonexistent"));
        Response response = base.request().get();

        Assert.assertEquals(Status.HTTP_VERSION_NOT_SUPPORTED.getStatusCode(), response.getStatus());

        response.close();
    }

    /**
     * @tpTestDetails Check correct path
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testMapper() throws Exception {
        WebTarget base = client.target(generateUrl("/test"));
        Response response = base.request().get();

        Assert.assertEquals(Response.Status.PRECONDITION_FAILED.getStatusCode(), response.getStatus());

        response.close();
    }

    /**
     * @tpTestDetails Check correct path, no content is excepted
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testMapper2() throws Exception {
        WebTarget base = client.target(generateUrl("/test/null"));
        Response response = base.request().get();

        Assert.assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());

        response.close();
    }

}
