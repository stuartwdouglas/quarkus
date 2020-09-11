package io.quarkus.rest.test.form;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.spi.HttpResponseCodes;
import io.quarkus.rest.test.form.resource.ResteasyUseContainerFormParamsFilter;
import io.quarkus.rest.test.form.resource.ResteasyUseContainerFormParamsResource;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import io.quarkus.rest.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.rest.test.simple.TestUtil;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * @tpSubChapter Configuration switches
 * @tpChapter Installation/Configuration
 * @tpSince RESTEasy 4.5.3
 * Show use of context parameter resteasy.use.container.form.params
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
        contextParam.put("resteasy.use.container.form.params","true");
        return TestUtil.finishContainerPrepare(war, contextParam, null);
    }});

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
        Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
    }
    @Test
    public void testMap() throws Exception {
        Builder builder = client.target(generateURL("/map"))
                .request();
        Response response = builder.post(Entity.form(
                new Form("hello", "world").param("yo", "mama")));
        Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
    }
}
