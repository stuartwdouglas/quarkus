package io.quarkus.rest.test.core.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.core.basic.resource.PrivateConstructorServiceResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Constructors
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-489
 * @tpSince RESTEasy 3.0.16
 */
public class PrivateConstructorTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, PrivateConstructorServiceResource.class);
                }
            });

    /**
     * @tpTestDetails Exception should not be thrown on WS with a non-public constructor
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testMapper() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        WebTarget base = client.target(PortProviderUtil.generateURL("/test", PrivateConstructorTest.class.getSimpleName()));
        Response response = base.request().get();
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        response.close();
        client.close();
    }
}
