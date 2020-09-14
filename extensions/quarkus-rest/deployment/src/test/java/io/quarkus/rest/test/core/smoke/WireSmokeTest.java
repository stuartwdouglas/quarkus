package io.quarkus.rest.test.core.smoke;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.core.smoke.resource.WireSmokeLocatingResource;
import io.quarkus.rest.test.core.smoke.resource.WireSmokeSimpleResource;
import io.quarkus.rest.test.core.smoke.resource.WireSmokeSimpleSubresource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Smoke tests for jaxrs
 * @tpChapter Integration tests
 * @tpTestCaseDetails Check basic resource function.
 * @tpSince RESTEasy 3.0.16
 */
public class WireSmokeTest {

    static Client client;
    private static final String WRONG_RESPONSE = "Wrong response content.";

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, WireSmokeLocatingResource.class,
                            WireSmokeSimpleResource.class, WireSmokeSimpleSubresource.class);
                }
            });

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, WireSmokeSimpleResource.class);
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

    private String generateURLNoDefault(String path) {
        return PortProviderUtil.generateURL(path, WireSmokeSimpleResource.class.getSimpleName());
    }

    private String generateURLLocating(String path) {
        return PortProviderUtil.generateURL(path, WireSmokeLocatingResource.class.getSimpleName());
    }

    /**
     * @tpTestDetails Check result from simple resource.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testNoDefaultsResource() throws Exception {
        {
            Response response = client.target(generateURLNoDefault("/basic")).request().get();
            Assert.assertEquals(Status.OK, response.getStatus());
            Assert.assertEquals(WRONG_RESPONSE, "basic", response.readEntity(String.class));
        }
        {
            Response response = client.target(generateURLNoDefault("/basic")).request()
                    .put(Entity.entity("basic", "text/plain"));
            Assert.assertEquals(Status.NO_CONTENT, response.getStatus());
            response.close();
        }
        {
            Response response = client.target(generateURLNoDefault("/queryParam")).queryParam("param", "hello world").request()
                    .get();
            Assert.assertEquals(Status.OK, response.getStatus());
            Assert.assertEquals(WRONG_RESPONSE, "hello world", response.readEntity(String.class));
        }
        {
            Response response = client.target(generateURLNoDefault("/uriParam/1234")).request().get();
            Assert.assertEquals(Status.OK, response.getStatus());
            Assert.assertEquals(WRONG_RESPONSE, "1234", response.readEntity(String.class));
        }
    }

    /**
     * @tpTestDetails Check result from more resources.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testLocatingResource() throws Exception {
        {
            Response response = client.target(generateURLLocating("/locating/basic")).request().get();
            Assert.assertEquals(Status.OK, response.getStatus());
            Assert.assertEquals(WRONG_RESPONSE, "basic", response.readEntity(String.class));
        }
        {
            Response response = client.target(generateURLLocating("/locating/basic")).request()
                    .put(Entity.entity("basic", "text/plain"));
            Assert.assertEquals(Status.NO_CONTENT, response.getStatus());
            response.close();
        }
        {
            Response response = client.target(generateURLLocating("/locating/queryParam")).queryParam("param", "hello world")
                    .request().get();
            Assert.assertEquals(Status.OK, response.getStatus());
            Assert.assertEquals(WRONG_RESPONSE, "hello world", response.readEntity(String.class));
        }
        {
            Response response = client.target(generateURLLocating("/locating/uriParam/1234")).queryParam("param", "hello world")
                    .request().get();
            Assert.assertEquals(Status.OK, response.getStatus());
            Assert.assertEquals(WRONG_RESPONSE, "1234", response.readEntity(String.class));
        }
        {
            Response response = client.target(generateURLLocating("/locating/uriParam/x1234"))
                    .queryParam("param", "hello world").request().get();
            Assert.assertEquals(Status.NOT_FOUND, response.getStatus());
            response.close();
        }
        {
            Response response = client.target(generateURLLocating("/locating/notmatching")).request().get();
            Assert.assertEquals(Status.NOT_FOUND, response.getStatus());
            response.close();
        }
        {
            Response response = client.target(generateURLLocating("/subresource/subresource/subresource/basic")).request()
                    .get();
            Assert.assertEquals(Status.OK, response.getStatus());
            Assert.assertEquals("basic", response.readEntity(String.class));
        }
    }
}
