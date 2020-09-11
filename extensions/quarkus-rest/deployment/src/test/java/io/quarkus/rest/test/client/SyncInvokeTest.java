package io.quarkus.rest.test.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.Supplier;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.client.resource.SyncInvokeResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpSince RESTEasy 3.0.16
 */
public class SyncInvokeTest extends ClientTestBase {

    @java.lang.annotation.Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @HttpMethod("PATCH")
    public @interface PATCH {
    }

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(SyncInvokeTest.class);
                    return TestUtil.finishContainerPrepare(war, null, SyncInvokeResource.class);
                }
            });

    @Before
    public void init() {
        client = ClientBuilder.newClient();
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Client sends GET, PUT, DELETE, POST and custom defined requests. First request expects
     *                Response object in return, the second expects String object in return
     * @tpPassCrit Successful response is returned
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testMethods() throws Exception {
        {
            Response res = client.target(generateURL("/test")).request().get();
            Assert.assertEquals(Status.OK, res.getStatus());
            String entity = res.readEntity(String.class);
            Assert.assertEquals("get", entity);
        }
        {
            String entity = client.target(generateURL("/test")).request().get(String.class);
            Assert.assertEquals("get", entity);
        }
        {
            Response res = client.target(generateURL("/test")).request().delete();
            Assert.assertEquals(Status.OK, res.getStatus());
            String entity = res.readEntity(String.class);
            Assert.assertEquals("delete", entity);
        }
        {
            String entity = client.target(generateURL("/test")).request().delete(String.class);
            Assert.assertEquals("delete", entity);
        }
        {
            Response res = client.target(generateURL("/test")).request().put(Entity.text("hello"));
            Assert.assertEquals(Status.OK, res.getStatus());
            String entity = res.readEntity(String.class);
            Assert.assertEquals("put hello", entity);
        }
        {
            String entity = client.target(generateURL("/test")).request().put(Entity.text("hello"), String.class);
            Assert.assertEquals("put hello", entity);
        }
        {
            Response res = client.target(generateURL("/test")).request().post(Entity.text("hello"));
            Assert.assertEquals(Status.OK, res.getStatus());
            String entity = res.readEntity(String.class);
            Assert.assertEquals("post hello", entity);
        }
        {
            String entity = client.target(generateURL("/test")).request().post(Entity.text("hello"), String.class);
            Assert.assertEquals("post hello", entity);
        }
        {
            Response res = client.target(generateURL("/test")).request().method("PATCH", Entity.text("hello"));
            Assert.assertEquals(Status.OK, res.getStatus());
            String entity = res.readEntity(String.class);
            Assert.assertEquals("patch hello", entity);
        }
        {
            String entity = client.target(generateURL("/test")).request().method("PATCH", Entity.text("hello"), String.class);
            Assert.assertEquals("patch hello", entity);
        }
    }

    /**
     * @tpTestDetails Client sends GET, PUT, DELETE, POST and custom defined requests. The request is send using
     *                invoke() method. First request expects Response object in return, the second expects String object in
     *                return
     * @tpPassCrit Successful response is returned
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testInvoke() throws Exception {
        {
            Response res = client.target(generateURL("/test")).request().buildGet().invoke();
            Assert.assertEquals(Status.OK, res.getStatus());
            String entity = res.readEntity(String.class);
            Assert.assertEquals("get", entity);
        }
        {
            String entity = client.target(generateURL("/test")).request().buildGet().invoke(String.class);
            Assert.assertEquals("get", entity);
        }
        {
            Response res = client.target(generateURL("/test")).request().buildDelete().invoke();
            Assert.assertEquals(Status.OK, res.getStatus());
            String entity = res.readEntity(String.class);
            Assert.assertEquals("delete", entity);
        }
        {
            String entity = client.target(generateURL("/test")).request().buildDelete().invoke(String.class);
            Assert.assertEquals("delete", entity);
        }
        {
            Response res = client.target(generateURL("/test")).request().buildPut(Entity.text("hello")).invoke();
            Assert.assertEquals(Status.OK, res.getStatus());
            String entity = res.readEntity(String.class);
            Assert.assertEquals("put hello", entity);
        }
        {
            String entity = client.target(generateURL("/test")).request().buildPut(Entity.text("hello")).invoke(String.class);
            Assert.assertEquals("put hello", entity);
        }
        {
            Response res = client.target(generateURL("/test")).request().buildPost(Entity.text("hello")).invoke();
            Assert.assertEquals(Status.OK, res.getStatus());
            String entity = res.readEntity(String.class);
            Assert.assertEquals("post hello", entity);
        }
        {
            String entity = client.target(generateURL("/test")).request().buildPost(Entity.text("hello")).invoke(String.class);
            Assert.assertEquals("post hello", entity);
        }
        {
            Response res = client.target(generateURL("/test")).request().build("PATCH", Entity.text("hello")).invoke();
            Assert.assertEquals(Status.OK, res.getStatus());
            String entity = res.readEntity(String.class);
            Assert.assertEquals("patch hello", entity);
        }
        {
            String entity = client.target(generateURL("/test")).request().build("PATCH", Entity.text("hello"))
                    .invoke(String.class);
            Assert.assertEquals("patch hello", entity);
        }
    }
}
