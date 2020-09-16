package io.quarkus.rest.test.resource.path;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.resource.path.resource.UriParamsWithLocatorLocator;
import io.quarkus.rest.test.resource.path.resource.UriParamsWithLocatorLocator2;
import io.quarkus.rest.test.resource.path.resource.UriParamsWithLocatorResource;
import io.quarkus.rest.test.resource.path.resource.UriParamsWithLocatorResource2;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter UriParamsWithLocatorResource
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 * @tpTestCaseDetails Test that a locator and resource with same path params work
 */
public class UriParamsWithLocatorTest {
    static Client client;

    @BeforeClass
    public static void setup() throws Exception {
        client = ClientBuilder.newClient();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(UriParamsWithLocatorResource.class);
                    return TestUtil.finishContainerPrepare(war, null, UriParamsWithLocatorLocator.class);
                }
            });

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(UriParamsWithLocatorResource2.class);
                    return TestUtil.finishContainerPrepare(war, null, UriParamsWithLocatorLocator2.class);
                }
            });

    @AfterClass
    public static void close() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails CTest double ID as String in resource
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testDoubleId() throws Exception {
        Response response = client.target(PortProviderUtil.generateURL("/1/2", "one"))
                .request().get();
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails CTest double ID as PathSegment in resource
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testDoubleIdAsPathSegment() throws Exception {
        Response response = client.target(PortProviderUtil.generateURL("/1/2", "two"))
                .request().get();
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
    }
}
