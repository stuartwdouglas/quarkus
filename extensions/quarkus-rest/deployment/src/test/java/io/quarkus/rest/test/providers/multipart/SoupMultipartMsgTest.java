package io.quarkus.rest.test.providers.multipart;

import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;
import org.jboss.resteasy.plugins.providers.multipart.MultipartOutput;
import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.multipart.resource.Soup;
import io.quarkus.rest.test.providers.multipart.resource.SoupVendorResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

public class SoupMultipartMsgTest {
    protected final Logger logger = LogManager.getLogger(SoupMultipartMsgTest.class.getName());
    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(Soup.class);
                    war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

                    return TestUtil.finishContainerPrepare(war, null, SoupVendorResource.class);
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
        return PortProviderUtil.generateURL(path, SoupMultipartMsgTest.class.getSimpleName());
    }

    @Test
    public void testPostMsg() throws Exception {

        MultipartOutput multipartOutput = new MultipartOutput();
        multipartOutput.addPart(new Soup("Chicken Noodle"),
                MediaType.APPLICATION_XML_TYPE);
        multipartOutput.addPart(new Soup("Vegetable"),
                MediaType.APPLICATION_XML_TYPE);
        multipartOutput.addPart("Granny's Soups", MediaType.TEXT_PLAIN_TYPE);

        ResteasyWebTarget target = client.target(generateURL("/vendor/register/soups"));
        Entity<MultipartOutput> entity = Entity.entity(multipartOutput,
                new MediaType("multipart", "mixed"));
        Response response = target.request().post(entity);

        Assert.assertEquals(Status.OK, response.getStatus());
        String result = response.readEntity(String.class);

        if (result.startsWith("Failed")) {
            Assert.fail(result);
        }
    }

    @Test
    public void testSoupObj() throws Exception {
        getMessage("/vendor/soups/obj");
    }

    @Test
    public void testSoupResp() throws Exception {
        getMessage("/vendor/soups/resp");
    }

    private void getMessage(String path) throws Exception {
        ResteasyWebTarget target = client.target(generateURL(path));
        Response response = target.request().get();

        MultipartInput multipartInput = response.readEntity(MultipartInput.class);
        Assert.assertEquals(Status.OK, response.getStatus());

        List<String> controlList = SoupVendorResource.getControlList();

        for (InputPart inputPart : multipartInput.getParts()) {
            if (MediaType.APPLICATION_XML_TYPE.equals(inputPart.getMediaType())) {
                Soup c = inputPart.getBody(Soup.class, null);
                String name = c.getId();
                if (controlList.contains(name)) {
                    controlList.remove(name);
                }
            } else {
                String name = inputPart.getBody(String.class, null);
                if (controlList.contains(name)) {
                    controlList.remove(name);
                }
            }
        }

        // verify content and report to test
        StringBuilder sb = new StringBuilder();
        if (!controlList.isEmpty()) {
            sb.append("Failed: parts not found: ");
            for (Iterator<String> it = controlList.iterator(); it.hasNext();) {
                sb.append(it.next() + " ");
            }
            Assert.fail(sb.toString());
        }

    }

    @Test
    public void testSouplistObj() throws Exception {
        getGenericTypeMessage("/vendor/souplist/obj");
    }

    @Test
    public void testSouplistResp() throws Exception {
        getGenericTypeMessage("/vendor/souplist/resp");
    }

    private void getGenericTypeMessage(String path) throws Exception {
        ResteasyWebTarget target = client.target(generateURL(path));
        Response response = target.request().get();
        MultipartInput multipartInput = response.readEntity(MultipartInput.class);

        Assert.assertEquals(Status.OK, response.getStatus());
        List<String> controlList = SoupVendorResource.getControlList();

        GenericType<List<Soup>> gType = new GenericType<List<Soup>>() {
        };
        for (InputPart inputPart : multipartInput.getParts()) {
            if (MediaType.APPLICATION_XML_TYPE.equals(inputPart.getMediaType())) {
                // List<Soup> soupList = inputPart.getBody(gType.getRawType(), gType.getType());
                List<Soup> soupList = inputPart.getBody(gType);
                for (Soup soup : soupList) {
                    String name = soup.getId();
                    if (controlList.contains(name)) {
                        controlList.remove(name);
                    }
                }

            } else {
                String name = inputPart.getBody(String.class, null);
                if (controlList.contains(name)) {
                    controlList.remove(name);
                }
            }
        }

        // verify content and report to test
        StringBuilder sb = new StringBuilder();
        if (!controlList.isEmpty()) {
            sb.append("Failed: parts not found: ");
            for (Iterator<String> it = controlList.iterator(); it.hasNext();) {
                sb.append(it.next() + " ");
            }
            Assert.fail(sb.toString());
        }

    }

    @Test
    public void testSoupFile() throws Exception {

        ResteasyWebTarget target = client.target(generateURL("/vendor/soupfile"));
        Response response = target.request().get();
        Assert.assertEquals(Status.OK, response.getStatus());

        MultipartInput multipartInput = response.readEntity(MultipartInput.class);
        InputPart inputPart = multipartInput.getParts().get(0);
        String bodyStr = inputPart.getBodyAsString();
        Assert.assertTrue("Failed to return expected data.",
                bodyStr.contains("Vegetable"));
    }
}
