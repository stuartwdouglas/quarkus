package io.quarkus.rest.test.resource.path;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.resource.path.resource.EmailResource;
import io.quarkus.rest.test.resource.path.resource.PathParamCarResource;
import io.quarkus.rest.test.resource.path.resource.PathParamDigits;
import io.quarkus.rest.test.resource.path.resource.PathParamResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpTestCaseDetails Spec requires that HEAD and OPTIONS are handled in a default manner
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Path Param Test")
public class PathParamTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, PathParamDigits.class, PathParamResource.class,
                    PathParamCarResource.class, EmailResource.class);
        }
    });

    /**
     * @tpTestDetails Check 6 parameters on path.
     *                Client invokes GET on root resource at /PathParamTest;
     *                Verify that right Method is invoked using
     *                PathParam primitive type List<String>.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test 6")
    public void test6() throws Exception {
        String[] Headers = { "list=abcdef" };
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        for (String header : Headers) {
            Invocation.Builder request = client
                    .target(PortProviderUtil.generateURL("/PathParamTest/a/b/c/d/e/f", PathLimitedTest.class.getSimpleName()))
                    .request();
            request.header("Accept", "text/plain");
            Response response = request.get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals(header, response.readEntity(String.class));
        }
        client.close();
    }

    /**
     * @tpTestDetails Check digits on path
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test 178")
    public void test178() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        {
            Invocation.Builder request = client
                    .target(PortProviderUtil.generateURL("/digits/5150", PathLimitedTest.class.getSimpleName())).request();
            Response response = request.get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            response.close();
        }
        {
            Invocation.Builder request = client
                    .target(PortProviderUtil.generateURL("/digits/5150A", PathLimitedTest.class.getSimpleName())).request();
            Response response = request.get();
            Assertions.assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
            response.close();
        }
        client.close();
    }

    /**
     * @tpTestDetails Check example car resource
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Car Resource")
    public void testCarResource() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        Invocation.Builder request = client.target(PortProviderUtil
                .generateURL("/cars/mercedes/matrixparam/e55;color=black/2006", PathLimitedTest.class.getSimpleName()))
                .request();
        Response response = request.get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "A black 2006 mercedes e55");
        // This must be a typo.  Should be "A midnight blue 2006 Porsche 911 Carrera S".
        request = client.target(PortProviderUtil.generateURL("/cars/mercedes/pathsegment/e55;color=black/2006",
                PathLimitedTest.class.getSimpleName())).request();
        response = request.get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "A black 2006 mercedes e55");
        request = client.target(PortProviderUtil.generateURL("/cars/mercedes/pathsegments/e55/amg/year/2006",
                PathLimitedTest.class.getSimpleName())).request();
        response = request.get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "A 2006 mercedes e55 amg");
        request = client.target(PortProviderUtil.generateURL("/cars/mercedes/uriinfo/e55;color=black/2006",
                PathLimitedTest.class.getSimpleName())).request();
        response = request.get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "A black 2006 mercedes e55");
        client.close();
    }

    /**
     * @tpTestDetails Test email format on path
     * @tpSince RESTEasy 3.0.20
     */
    @Test
    @DisplayName("Test Email")
    public void testEmail() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        Response response = client.target(PortProviderUtil.generateURL("/employeeinfo/employees/bill.burke@burkecentral.com",
                PathLimitedTest.class.getSimpleName())).request().get();
        String str = response.readEntity(String.class);
        Assertions.assertEquals(str, "burke");
        client.close();
    }
}
