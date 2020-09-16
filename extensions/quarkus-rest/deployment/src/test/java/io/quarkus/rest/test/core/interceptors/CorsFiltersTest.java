package io.quarkus.rest.test.core.interceptors;

import static org.hamcrest.core.Is.is;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.plugins.interceptors.CorsFilter;
import org.jboss.resteasy.spi.CorsHeaders;
import org.jboss.resteasy.utils.TestApplication;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.core.interceptors.resource.CorsFiltersResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Interceptors
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test CorsFilter usage
 * @tpSince RESTEasy 3.0.16
 */

public class CorsFiltersTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(PortProviderUtil.class);

                    List<Class<?>> singletons = new ArrayList<>();
                    singletons.add(CorsFilter.class);
                    // Arquillian in the deployment and use of PortProviderUtil

                    return TestUtil.finishContainerPrepare(war, null, singletons, CorsFiltersResource.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, CorsFiltersTest.class.getSimpleName());
    }

    @After
    public void resetFilter() {
        CorsFilter corsFilter = (CorsFilter) TestApplication.singletons.iterator().next();
        corsFilter.getAllowedOrigins().remove("http://" + PortProviderUtil.getHost());
    }

    /**
     * @tpTestDetails Check different options of Cors headers.
     *                CorsFilter is created as singleton in TestApplication instance.
     *                In this test is CorsFilter get from static set from TestApplication class.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testPreflight() throws Exception {
        String testedURL = "http://" + PortProviderUtil.getHost();

        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        WebTarget target = client.target(generateURL("/test"));
        Response response = target.request().header(CorsHeaders.ORIGIN, testedURL)
                .options();
        Assert.assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
        response.close();
        response = target.request().header(CorsHeaders.ORIGIN, testedURL)
                .get();
        Assert.assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
        response.close();

        Assert.assertThat("Wrong count of singletons were created", TestApplication.singletons.size(), is(1));
        CorsFilter corsFilter = (CorsFilter) TestApplication.singletons.iterator().next();

        corsFilter.getAllowedOrigins().add(testedURL);
        response = target.request().header(CorsHeaders.ORIGIN, testedURL)
                .options();
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
        response = target.request().header(CorsHeaders.ORIGIN, testedURL)
                .get();
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals(response.getHeaderString(CorsHeaders.ACCESS_CONTROL_ALLOW_ORIGIN), testedURL);
        Assert.assertEquals("Wrong response", "hello", response.readEntity(String.class));
        response.close();

        client.close();
    }

    /**
     * @tpTestDetails Test that the response contains the Vary: Origin header
     * @tpInfo RESTEASY-1704
     * @tpSince RESTEasy 3.0.25
     */
    @Test
    public void testVaryOriginHeader() {
        String testedURL = "http://" + PortProviderUtil.getHost();
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        WebTarget target = client.target(generateURL("/test"));

        Assert.assertThat("Wrong count of singletons were created", TestApplication.singletons.size(), is(1));
        CorsFilter corsFilter = (CorsFilter) TestApplication.singletons.iterator().next();
        corsFilter.getAllowedOrigins().add(testedURL);

        Response response = target.request().header(CorsHeaders.ORIGIN, testedURL).get();
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals("Response doesn't contain the Vary: Origin header", CorsHeaders.ORIGIN,
                response.getHeaderString(CorsHeaders.VARY));
        response.close();

        response = target.request().header(CorsHeaders.ORIGIN, testedURL).options();
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals("Response doesn't contain the Vary: Origin header", CorsHeaders.ORIGIN,
                response.getHeaderString(CorsHeaders.VARY));
        response.close();

        client.close();
    }

}
