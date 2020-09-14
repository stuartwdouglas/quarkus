package io.quarkus.rest.test.form;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.form.resource.ResteasyUseContainerFormParamsFilter;
import io.quarkus.rest.test.form.resource.ResteasyUseContainerFormParamsResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Configuration switches
 * @tpChapter Installation/Configuration
 * @tpSince RESTEasy 4.5.3
 *          Show use of context parameter resteasy.use.container.form.params
 */
public class ResteasyUseContainerFormParamsTest {
    private static Client client;
    private static String testSimpleName = ResteasyUseContainerFormParamsTest.class.getSimpleName();
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(ResteasyUseContainerFormParamsResource.class,
                            ResteasyUseContainerFormParamsFilter.class);
                    Map<String, String> contextParam = new HashMap<>();
                    contextParam.put("resteasy.use.container.form.params", "true");
                    return TestUtil.finishContainerPrepare(war, contextParam, null);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, testSimpleName);
    }

    @BeforeClass
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
    }

    @AfterClass
    public static void after() throws Exception {
        client.close();
    }

    @Test
    public void testForm() throws Exception {
        Builder builder = client.target(generateURL("/form")).request();
        Response response = builder.post(Entity.form(
                new Form("hello", "world").param("yo", "mama")));
        Assert.assertEquals(Status.OK, response.getStatus());
    }

    @Test
    public void testMap() throws Exception {
        Builder builder = client.target(generateURL("/map"))
                .request();
        Response response = builder.post(Entity.form(
                new Form("hello", "world").param("yo", "mama")));
        Assert.assertEquals(Status.OK, response.getStatus());
    }
}
