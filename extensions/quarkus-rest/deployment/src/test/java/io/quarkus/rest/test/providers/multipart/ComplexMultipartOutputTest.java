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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.providers.multipart.resource.ComplexMultipartOutputResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

@DisplayName("Complex Multipart Output Test")
public class ComplexMultipartOutputTest {

    protected final Logger logger = Logger.getLogger(ComplexMultipartOutputTest.class.getName());

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, ComplexMultipartOutputResource.class);
        }
    });

    @BeforeAll
    public static void before() throws Exception {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterAll
    public static void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ComplexMultipartOutputTest.class.getSimpleName());
    }

    @Test
    @DisplayName("Test Get Complex Case")
    public void testGetComplexCase() throws Exception {
        List<String> controlList = new ArrayList<>();
        controlList.add("bill");
        controlList.add("bob");
        QuarkusRestWebTarget target = client.target(generateURL("/mpart/test"));
        Response response = target.request().get();
        MultipartInput multipartInput = response.readEntity(MultipartInput.class);
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // debug
        List<InputPart> parts = multipartInput.getParts();
        Assertions.assertEquals(2, parts.size());
        for (InputPart inputPart : multipartInput.getParts()) {
            MultipartRelatedInput mRelatedInput = inputPart.getBody(MultipartRelatedInput.class, null);
            Assertions.assertEquals(1, mRelatedInput.getRelatedMap().size());
            for (Map.Entry<String, InputPart> entry : mRelatedInput.getRelatedMap().entrySet()) {
                String key = entry.getKey();
                InputPart iPart = entry.getValue();
                if (controlList.contains(key)) {
                    controlList.remove(key);
                }
                if (iPart instanceof MultipartInputImpl.PartImpl) {
                    MultipartInputImpl.PartImpl miPart = (MultipartInputImpl.PartImpl) iPart;
                    InputStream inStream = miPart.getBody();
                    Assertions.assertNotNull(inStream, "InputStream should not be null.");
                }
            }
        }
        if (!controlList.isEmpty()) {
            Assertions.fail("1 or more missing MultipartRelatedInput return objects");
        }
    }

    @Test
    @DisplayName("Test Post Complex Case")
    public void testPostComplexCase() throws Exception {
        MultipartRelatedOutput mRelatedOutput = new MultipartRelatedOutput();
        mRelatedOutput.setStartInfo("text/html");
        mRelatedOutput.addPart("Bill", new MediaType("image", "png"), "bill", "binary");
        mRelatedOutput.addPart("Bob", new MediaType("image", "png"), "bob", "binary");
        WebTarget target = client.target(generateURL("/mpart/post/related"));
        Entity<MultipartRelatedOutput> entity = Entity.entity(mRelatedOutput, new MediaType("multipart", "related"));
        Response response = target.request().post(entity);
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        MultipartRelatedInput result = response.readEntity(MultipartRelatedInput.class);
        Set<String> keys = result.getRelatedMap().keySet();
        Assertions.assertTrue(keys.size() == 2);
        Assertions.assertTrue(keys.contains("Bill"), "Failed to find inputPart Bill");
        Assertions.assertTrue(keys.contains("Bob"), "Failed to find inputPart Bob");
    }
}
