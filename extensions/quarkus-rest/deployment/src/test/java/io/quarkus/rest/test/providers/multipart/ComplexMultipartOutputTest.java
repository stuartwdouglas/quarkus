package io.quarkus.rest.test.providers.multipart;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;
import org.jboss.resteasy.plugins.providers.multipart.MultipartInputImpl;
import org.jboss.resteasy.plugins.providers.multipart.MultipartRelatedInput;
import org.jboss.resteasy.plugins.providers.multipart.MultipartRelatedOutput;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.providers.multipart.resource.ComplexMultipartOutputResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

public class ComplexMultipartOutputTest {
    protected final Logger logger = Logger.getLogger(
            ComplexMultipartOutputTest.class.getName());

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null,
                            ComplexMultipartOutputResource.class);
                }
            });

    @BeforeClass
    public static void before() throws Exception {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterClass
    public static void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path,
                ComplexMultipartOutputTest.class.getSimpleName());
    }

    @Test
    public void testGetComplexCase() throws Exception {

        List<String> controlList = new ArrayList<>();
        controlList.add("bill");
        controlList.add("bob");

        QuarkusRestWebTarget target = client.target(generateURL("/mpart/test"));
        Response response = target.request().get();
        MultipartInput multipartInput = response.readEntity(MultipartInput.class);
        Assert.assertEquals(Status.OK, response.getStatus());

        List<InputPart> parts = multipartInput.getParts(); // debug
        Assert.assertEquals(2, parts.size());

        for (InputPart inputPart : multipartInput.getParts()) {

            MultipartRelatedInput mRelatedInput = inputPart.getBody(
                    MultipartRelatedInput.class, null);
            Assert.assertEquals(1, mRelatedInput.getRelatedMap().size());

            for (Map.Entry<String, InputPart> entry : mRelatedInput
                    .getRelatedMap().entrySet()) {
                String key = entry.getKey();
                InputPart iPart = entry.getValue();

                if (controlList.contains(key)) {
                    controlList.remove(key);
                }

                if (iPart instanceof MultipartInputImpl.PartImpl) {
                    MultipartInputImpl.PartImpl miPart = (MultipartInputImpl.PartImpl) iPart;
                    InputStream inStream = miPart.getBody();
                    Assert.assertNotNull(
                            "InputStream should not be null.", inStream);
                }
            }

        }

        if (!controlList.isEmpty()) {
            Assert.fail("1 or more missing MultipartRelatedInput return objects");
        }
    }

    @Test
    public void testPostComplexCase() throws Exception {
        MultipartRelatedOutput mRelatedOutput = new MultipartRelatedOutput();
        mRelatedOutput.setStartInfo("text/html");
        mRelatedOutput.addPart("Bill", new MediaType("image",
                "png"), "bill", "binary");
        mRelatedOutput.addPart("Bob", new MediaType("image",
                "png"), "bob", "binary");

        WebTarget target = client.target(generateURL("/mpart/post/related"));
        Entity<MultipartRelatedOutput> entity = Entity.entity(mRelatedOutput,
                new MediaType("multipart", "related"));
        Response response = target.request().post(entity);
        Assert.assertEquals(Status.OK, response.getStatus());

        MultipartRelatedInput result = response.readEntity(MultipartRelatedInput.class);
        Set<String> keys = result.getRelatedMap().keySet();
        Assert.assertTrue(keys.size() == 2);
        Assert.assertTrue("Failed to find inputPart Bill", keys.contains("Bill"));
        Assert.assertTrue("Failed to find inputPart Bob", keys.contains("Bob"));
    }
}
