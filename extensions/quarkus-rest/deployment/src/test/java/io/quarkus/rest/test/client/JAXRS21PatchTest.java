package io.quarkus.rest.test.client;

import java.util.function.Supplier;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.hamcrest.CoreMatchers;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.Assert;
import io.quarkus.rest.test.client.resource.JAXRS21SyncInvokeResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

public class JAXRS21PatchTest extends ClientTestBase {

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(JAXRS21PatchTest.class);
                    return TestUtil.finishContainerPrepare(war, null, JAXRS21SyncInvokeResource.class);
                }
            });

    @BeforeEach
    public void init() {
        client = ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Client sends http PATCH request with invoke() method
     * @tpSince RESTEasy 3.5.0
     */
    @Test
    public void testMethods() throws Exception {
        {
            Response res = client.target(generateURL("/test")).request().method(HttpMethod.PATCH, Entity.text("hello"));
            Assert.assertEquals(Status.OK.getStatusCode(), res.getStatus());
            String entity = res.readEntity(String.class);
            Assert.assertEquals("patch hello", entity);
        }
        {
            String entity = client.target(generateURL("/test")).request().method(HttpMethod.PATCH, Entity.text("hello"),
                    String.class);
            Assert.assertEquals("patch hello", entity);
        }
    }

    /**
     * @tpTestDetails Client sends http PATCH request with method() method
     * @tpSince RESTEasy 3.5.0
     */
    @Test
    public void testInvoke() throws Exception {
        {
            Response res = client.target(generateURL("/test")).request().build(HttpMethod.PATCH, Entity.text("hello")).invoke();
            Assert.assertEquals(Status.OK.getStatusCode(), res.getStatus());
            String entity = res.readEntity(String.class);
            Assert.assertEquals("patch hello", entity);
        }
        {
            String entity = client.target(generateURL("/test")).request().build(HttpMethod.PATCH, Entity.text("hello"))
                    .invoke(String.class);
            Assert.assertEquals("patch hello", entity);
        }
    }

    /**
     * @tpTestDetails Check that PATCH is present in OPTIONS response if the resource supports PATCH method
     * @tpSince RESTEasy 3.5.0
     */
    @Test
    public void testOptionsContainsAllowPatch() throws Exception {
        Response res = client.target(generateURL("/test")).request().options();
        Assert.assertThat(res.getHeaderString("Allow"), CoreMatchers.containsString("PATCH"));
        res.close();
    }

    /**
     * @tpTestDetails Check that OPTIONS response contains Accept-Patch header with supported PATCH format descriptors
     * @tpSince RESTEasy 3.5.0
     */
    @Test
    public void testOptionsContainsAcceptPatch() throws Exception {
        Response res = client.target(generateURL("/test")).request().options();
        Assert.assertEquals("text/plain", res.getHeaderString("Accept-Patch"));
        res.close();
    }

    /**
     * @tpTestDetails Check http headers in the response after successful PATCH request
     * @tpSince RESTEasy 3.5.0
     */
    @Test
    public void testPatchHeaders() throws Exception {
        Response res = client.target(generateURL("/test")).request().method(HttpMethod.PATCH, Entity.text("hello"));
        MultivaluedMap<String, String> stringHeaders = res.getStringHeaders();
        stringHeaders.forEach((k, v) -> {
            if (k.equals("Content-type"))
                Assert.assertEquals("text/plain;charset=UTF-8", v);
        });
    }
}
